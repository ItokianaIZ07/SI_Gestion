//Brandon

package com.si.rh.service;

import com.si.rh.dto.*;
import com.si.rh.model.*;
import com.si.rh.model.enums.*;
import com.si.rh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratService {

    private final CandidatureRepository candidatureRepo;
    private final EmployeRepository employeRepo;
    private final ContratRepository contratRepo;
    private final HistoriqueContratRepository historiqueRepo;
    private final OnboardingRepository onboardingRepo;
    private final TacheOnboardingRepository tacheRepo;
    private final ModeleTacheOnboardingRepository modeleRepo;
    private final TypeContratRepository typeContratRepo;
    private final StatutContractRepository statutRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final EmailService emailService;

    // ============ Lecture ============

    @Transactional(readOnly = true)
    public List<CandidatAEmbaucherDTO> getCandidatsAEmbaucher() {
        return candidatureRepo.findRetenusSansEmploye().stream()
            .map(c -> new CandidatAEmbaucherDTO(
                c.getId(),
                c.getCandidat().getNom(),
                c.getCandidat().getPrenom(),
                c.getCandidat().getEmail(),
                c.getRecrutement().getBesoin().getPoste().getNom(),
                c.getRecrutement().getBesoin().getPoste().getDepartement().getNom(),
                c.getRecrutement().getBesoin().getSalaireEstime(),
                c.getRecrutement().getBesoin().getTypeContratSouhaite() == null
                    ? "CDI"
                    : c.getRecrutement().getBesoin().getTypeContratSouhaite().getCode(),
                c.getRecrutement().getBesoin().getPoste().getId(),
                c.getRecrutement().getBesoin().getPoste().getDepartement().getId()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ContratDTO> getContrats() {
        return contratRepo.findActifsEtEnCours().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public ContratDTO getContrat(Integer id) {
        return contratRepo.findByIdAvecDetails(id)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable"));
    }

    // ============ Embauche (transactionnelle) ============

    @Transactional
    public ContratDTO embaucher(EmbaucheRequest req, Integer idUtilisateurCreateur) {
        Candidature c = candidatureRepo.findByIdAvecDetails(req.getIdCandidature())
            .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));

        if (c.getStatut() != StatutCandidature.RETENU)
            throw new IllegalStateException("La candidature doit être RETENU.");
        if (employeRepo.existsByIdCandidature(c.getId()))
            throw new IllegalStateException("Un employé existe déjà pour cette candidature.");

        Besoin b = c.getRecrutement().getBesoin();
        Poste p = b.getPoste();
        Departement dep = p.getDepartement();
        Utilisateur createur = utilisateurRepo.findById(idUtilisateurCreateur)
            .orElseThrow(() -> new IllegalArgumentException("Créateur introuvable"));

        // 1. Employé
        Employe e = new Employe();
        e.setMatricule("MAT-" + String.format("%05d", employeRepo.count() + 1));
        e.setNom(c.getCandidat().getNom());
        e.setPrenom(c.getCandidat().getPrenom());
        e.setEmail(c.getCandidat().getEmail());
        e.setTelephone(c.getCandidat().getTelephone());
        e.setNumeroCin(c.getCandidat().getNumeroCin());
        e.setCandidature(c);
        e.setDepartement(dep);
        e.setPoste(p);
        e.setDateEmbauche(req.getDateDebut());
        e.setStatut(StatutEmploye.ACTIF);
        employeRepo.save(e);

        // 2. Contrat
        TypeContrat tc = typeContratRepo.findByCode(req.getTypeContrat())
            .orElseThrow(() -> new IllegalArgumentException("Type de contrat inconnu"));
        StatutContract sc = statutRepo.findByCode("EN_ATTENTE_SIGNATURE")
            .orElseThrow(() -> new IllegalStateException("Statut EN_ATTENTE_SIGNATURE absent"));

        Contrat ct = new Contrat();
        ct.setReference("CTR-" + req.getDateDebut().getYear() + "-"
                        + String.format("%05d", e.getId()));
        ct.setEmploye(e);
        ct.setTypeContrat(tc);
        ct.setStatutContract(sc);
        ct.setCreateur(createur);
        ct.setDateDebut(req.getDateDebut());
        if (tc.getDuree() != null)
            ct.setDateFin(req.getDateDebut().plusDays(tc.getDuree()));
        ct.setPeriodeEssaiJours(req.getPeriodeEssaiJours());
        ct.setSalaire(req.getSalaire());
        contratRepo.save(ct);

        // 3. Historique : création
        historiqueRepo.save(historique(ct, null, sc, createur,
            "CREATION", "Création du contrat"));

        // 4. Onboarding + tâches
        Onboarding o = new Onboarding();
        o.setEmploye(e);
        o.setResponsable(createur);
        o.setDateDebut(req.getDateDebut());
        o.setDateFinPrevue(req.getDateDebut().plusDays(30));
        o.setStatut(StatutOnboarding.A_FAIRE);
        onboardingRepo.save(o);

        for (ModeleTacheOnboarding m : modeleRepo.findByActifTrue()) {
            TacheOnboarding t = new TacheOnboarding();
            t.setOnboarding(o);
            t.setModele(m);
            t.setResponsable(createur);
            t.setLibelle(m.getLibelle());
            t.setCategorie(m.getCategorie());
            t.setDateEcheance(req.getDateDebut().plusDays(m.getDelaiJours()));
            t.setStatut(StatutTache.A_FAIRE);
            tacheRepo.save(t);
        }

        // 5. E-mail
        emailService.envoyerContratASigner(
            c.getCandidat().getEmail(), c.getCandidat().getPrenom(), ct.getReference());

        return toDTO(ct);
    }

    // ============ Signature ============

    @Transactional
    public ContratDTO signer(Integer idContrat, String partie) {
        Contrat ct = contratRepo.findById(idContrat)
            .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable"));
        StatutContract actuel = ct.getStatutContract();

        if ("BROUILLON".equals(actuel.getCode()))
            throw new IllegalStateException("Le contrat est encore en brouillon.");
        if ("ACTIF".equals(actuel.getCode()))
            throw new IllegalStateException("Le contrat est déjà actif.");

        StatutContract actif = statutRepo.findByCode("ACTIF")
            .orElseThrow(() -> new IllegalStateException("Statut ACTIF absent"));

        historiqueRepo.save(historique(ct, actuel, actuel, null,
            "SIGNATURE_" + partie, "Signature " + partie));

        long signatures = historiqueRepo
            .countByEvenementContainingAndContratId("SIGNATURE_", ct.getId());

        if (signatures >= 2) {
            ct.setStatutContract(actif);
            ct.setDateSignature(java.time.LocalDate.now());
            contratRepo.save(ct);
            historiqueRepo.save(historique(ct, actuel, actif, null,
                "ACTIVATION", "Contrat signé des deux parties"));
        }

        return toDTO(ct);
    }

    // ============ Helpers ============

    private HistoriqueContrat historique(Contrat ct, StatutContract ancien,
                                         StatutContract nouveau, Utilisateur auteur,
                                         String evenement, String motif) {
        HistoriqueContrat h = new HistoriqueContrat();
        h.setContrat(ct);
        h.setAncienStatut(ancien);
        h.setStatutContract(nouveau);
        h.setAuteur(auteur);
        h.setEvenement(evenement);
        h.setMotif(motif);
        h.setDateModification(OffsetDateTime.now());
        return h;
    }

    private ContratDTO toDTO(Contrat c) {
        Employe e = c.getEmploye();
        return new ContratDTO(
            c.getId(),
            c.getReference(),
            e == null ? null : e.getId(),
            e == null ? null : e.getNom(),
            e == null ? null : e.getPrenom(),
            c.getTypeContrat() == null ? null : c.getTypeContrat().getCode(),
            c.getStatutContract() == null ? null : c.getStatutContract().getCode(),
            c.getDateDebut(),
            c.getDateFin(),
            c.getDateSignature(),
            c.getSalaire(),
            c.getDateCreation()
        );
    }
}
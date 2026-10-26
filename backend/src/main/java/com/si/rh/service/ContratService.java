//Brandon

package com.si.rh.service;

import com.si.rh.dto.*;
import com.si.rh.model.*;
import com.si.rh.model.enums.*;
import com.si.rh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Base64;

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
    private final PosteRepository posteRepo;
    private final EmailService emailService;
    private final SignatureTokenService signatureTokenService;

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

        // 5. E-mail : informer le candidat que son contrat est en préparation
        String nomPoste = p.getNom();
        emailService.envoyerContratEnPreparation(
            c.getCandidat().getEmail(),
            c.getCandidat().getPrenom(),
            nomPoste,
            ct.getReference()
        );

        return toDTO(ct);
    }

    // ============ Signature ============

    @Transactional
    public ContratDTO signer(Integer idContrat, String partie, String signatureBase64) {
        Contrat ct = contratRepo.findById(idContrat)
            .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable"));
        StatutContract actuel = ct.getStatutContract();

        if ("BROUILLON".equals(actuel.getCode()))
            throw new IllegalStateException("Le contrat est encore en brouillon.");
        if ("ACTIF".equals(actuel.getCode()))
            throw new IllegalStateException("Le contrat est déjà actif.");

        StatutContract actif = statutRepo.findByCode("ACTIF")
            .orElseThrow(() -> new IllegalStateException("Statut ACTIF absent"));

        HistoriqueContrat h = historique(ct, actuel, actuel, null,
            "SIGNATURE_" + partie, "Signature " + partie);

        // Décoder et stocker l'image si présente
        if (signatureBase64 != null && !signatureBase64.isEmpty()) {
            try {
                String base64 = signatureBase64.replace("data:image/png;base64,", "");
                h.setSignatureImage(Base64.getDecoder().decode(base64));
            } catch (IllegalArgumentException ex) {
                System.err.println("[SIGNATURE] base64 invalide : " + ex.getMessage());
            }
        }
        historiqueRepo.save(h);

        long signatures = historiqueRepo
            .countByEvenementContainingAndContratId("SIGNATURE_", ct.getId());

        // ---- 1ère signature (RH) → envoyer le lien au candidat
        if ("RH".equals(partie) && signatures < 2) {
            Employe e = ct.getEmploye();
            if (e == null)
                throw new IllegalArgumentException("Employé introuvable");

            Poste p = e.getPoste();
            String nomPoste = p != null ? p.getNom() : "votre poste";

            signatureTokenService.genererEtEnvoyerLienApresRH(
                ct.getId(), e.getEmail(), e.getPrenom(), nomPoste);
        }

        // ---- 2 signatures → activer + email de confirmation
        if (signatures >= 2) {
            ct.setStatutContract(actif);
            ct.setDateSignature(LocalDate.now());
            contratRepo.save(ct);
            historiqueRepo.save(historique(ct, actuel, actif, null,
                "ACTIVATION", "Contrat signé des deux parties"));

            Employe e = ct.getEmploye();
            if (e != null) {
                Poste p = e.getPoste();
                String nomPoste = p != null ? p.getNom() : "votre poste";
                emailService.envoyerConfirmationContratActif(
                    e.getEmail(), e.getPrenom(), ct.getReference(), nomPoste);
            }
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

    // BY : Brandon
    private ContratDTO toDTO(Contrat c) {
        Employe e = c.getEmploye();
        long nb = historiqueRepo.countByEvenementContainingAndContratId("SIGNATURE_", c.getId());

        // Champs nullables : si l'employé n'a pas renseigné CIN/date naissance/adresse,
        // on renvoie null et le front affichera une chaîne vide.
        String cin        = e == null ? null : e.getNumeroCin();
        LocalDate naiss   = e == null ? null : e.getDateNaissance();
        String adresse    = e == null ? null : e.getAdresse();
        String email      = e == null ? null : e.getEmail();
        String telephone  = e == null ? null : e.getTelephone();

        String posteNom   = (e == null || e.getPoste() == null) ? null : e.getPoste().getNom();
        String deptNom    = (e == null || e.getDepartement() == null) ? null : e.getDepartement().getNom();

        String duree      = c.getTypeContrat() == null || c.getTypeContrat().getDuree() == null
                            ? null
                            : c.getTypeContrat().getDuree() + " jours";

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
            c.getDateCreation(),
            (int) nb,
            cin,
            naiss,
            adresse,
            email,
            telephone,
            posteNom,
            deptNom,
            duree,
            c.getPeriodeEssaiJours()
        );
    }

    @Transactional
    public ContratDTO signerCandidat(Integer idContrat,String signatureBase64) {
        Contrat ct = contratRepo.findById(idContrat)
            .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable"));

        StatutContract actuel = ct.getStatutContract();
        if ("BROUILLON".equals(actuel.getCode()))
            throw new IllegalStateException("Contrat en brouillon");
        if ("ACTIF".equals(actuel.getCode()))
            throw new IllegalStateException("Contrat déjà actif");

        StatutContract actif = statutRepo.findByCode("ACTIF")
            .orElseThrow(() -> new IllegalStateException("Statut ACTIF absent"));

        // Enregistre la signature candidat
        HistoriqueContrat h = historique(ct, actuel, actuel, null,
            "SIGNATURE_CANDIDAT", "Signature du candidat via lien sécurisé");

        if (signatureBase64 != null && !signatureBase64.isEmpty()) {
            try {
                String base64 = signatureBase64.replace("data:image/png;base64,", "");
                h.setSignatureImage(Base64.getDecoder().decode(base64));
            } catch (IllegalArgumentException ex) {
                System.err.println("[SIGNATURE] base64 invalide : " + ex.getMessage());
            }
        }
        historiqueRepo.save(h);

        long signatures = historiqueRepo
            .countByEvenementContainingAndContratId("SIGNATURE_", ct.getId());

        if (signatures >= 2) {
            ct.setStatutContract(actif);
            ct.setDateSignature(LocalDate.now());
            contratRepo.save(ct);
            historiqueRepo.save(historique(ct, actuel, actif, null,
                "ACTIVATION", "Contrat signé par les deux parties"));

            Employe e = ct.getEmploye();
            if (e != null) {
                Poste p = e.getPoste();
                String nomPoste = p != null ? p.getNom() : "votre poste";
                emailService.envoyerConfirmationContratActif(
                    e.getEmail(), e.getPrenom(), ct.getReference(), nomPoste);
            }
        }

        return toDTO(ct);
    }

    @Transactional(readOnly = true)
    public byte[] getSignature(Integer idContrat, String partie) {
        String evt = "SIGNATURE_" + partie.toUpperCase();
        return historiqueRepo.findAll().stream()
            .filter(h -> h.getContrat().getId().equals(idContrat))
            .filter(h -> evt.equals(h.getEvenement()))
            .filter(h -> h.getSignatureImage() != null)
            .map(HistoriqueContrat::getSignatureImage)
            .findFirst()
            .orElse(null);
    }
}
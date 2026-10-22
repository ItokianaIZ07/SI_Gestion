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
public class EntretienService {

    private final CandidatureRepository candidatureRepo;
    private final EntretienRepository entretienRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final DecisionRecrutementRepository decisionRepo;
    private final CandidatRepository candidatRepo;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<CandidatureEntretienDTO> getCandidaturesAvecEntretiens() {
        return candidatureRepo.findPourEntretien().stream()
            .map(c -> {
                Entretien last = entretienRepo
                    .findByCandidatureIdOrderByDateEntretienDesc(c.getId())
                    .stream().findFirst().orElse(null);
                return new CandidatureEntretienDTO(
                    c.getId(),
                    c.getCandidat().getNom(),
                    c.getCandidat().getPrenom(),
                    c.getCandidat().getEmail(),
                    c.getRecrutement().getBesoin().getPoste().getNom(),
                    c.getRecrutement().getBesoin().getPoste().getDepartement().getNom(),
                    c.getStatut().name(),
                    last == null ? null : toDTO(last)
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IntervieweurDTO> getIntervieweurs() {
        return utilisateurRepo.findIntervieweurs().stream()
            .map(u -> new IntervieweurDTO(
                u.getId(), u.getNom(), u.getPrenom(), u.getRole().getCode()))
            .toList();
    }

    @Transactional
    public EntretienDTO enregistrer(EntretienRequest req) {
        Candidature c = candidatureRepo.findByIdAvecDetails(req.getIdCandidature())
            .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));

        Entretien e = (req.getId() != null)
            ? entretienRepo.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Entretien introuvable"))
            : new Entretien();

        e.setCandidature(c);
        e.setTypeEntretien(TypeEntretien.valueOf(req.getTypeEntretien()));
        e.setDateEntretien(req.getDateEntretien().atOffset(java.time.ZoneOffset.UTC));
        e.setLieu(req.getLieu());
        e.setStatut(StatutEntretien.PLANIFIE);
        if (req.getIdUtilisateurIntervieweur() != null) {
            Utilisateur u = utilisateurRepo.findById(req.getIdUtilisateurIntervieweur())
                .orElseThrow(() -> new IllegalArgumentException("Intervieweur introuvable"));
            e.setIntervieweur(u);
        }

        entretienRepo.save(e);
        return toDTO(e);
    }

    @Transactional(readOnly = true)
    public void convoquer(Integer idEntretien) {
        Entretien e = entretienRepo.findById(idEntretien)
            .orElseThrow(() -> new IllegalArgumentException("Entretien introuvable"));
        Candidature c = e.getCandidature();

        String nomPoste = c.getRecrutement().getBesoin().getPoste().getNom();
        emailService.envoyerConvocation(
            c.getCandidat().getEmail(), c.getCandidat().getPrenom(),
            e.getDateEntretien().toLocalDateTime(), e.getTypeEntretien().name(),
            e.getLieu(), nomPoste);
    }

    @Transactional
    public void notifierResultat(Integer idCandidature, String decision, Integer idDecideur) {
        Candidature c = candidatureRepo.findByIdAvecDetails(idCandidature)
            .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));

        c.setStatut("RETENU".equals(decision)
            ? StatutCandidature.RETENU
            : StatutCandidature.REJETE);
        candidatureRepo.save(c);

        Utilisateur decideur = utilisateurRepo.findById(idDecideur)
            .orElseThrow(() -> new IllegalArgumentException("Décideur introuvable"));

        DecisionRecrutement d = decisionRepo.findByCandidatureId(idCandidature)
            .orElseGet(DecisionRecrutement::new);
        d.setCandidature(c);
        d.setDecision(ValeurDecision.valueOf(decision));
        d.setDateDecision(OffsetDateTime.now());
        d.setDecideur(decideur);
        decisionRepo.save(d);

        try {
            String nomPoste = c.getRecrutement().getBesoin().getPoste().getNom();
            emailService.envoyerResultat(
                c.getCandidat().getEmail(), c.getCandidat().getPrenom(),
                nomPoste, decision);
        } catch (Exception ex) {
            System.err.println("[MAIL] Échec envoi résultat : " + ex.getMessage());
        }
    }

    private EntretienDTO toDTO(Entretien e) {
        String intervieweurNom = null;
        if (e.getIntervieweur() != null) {
            intervieweurNom = e.getIntervieweur().getPrenom() + " "
                            + e.getIntervieweur().getNom();
        }
        return new EntretienDTO(
            e.getId(),
            e.getCandidature() == null ? null : e.getCandidature().getId(),
            e.getTypeEntretien() == null ? null : e.getTypeEntretien().name(),
            e.getDateEntretien() == null ? null : e.getDateEntretien().toLocalDateTime(),
            e.getLieu(),
            e.getStatut() == null ? null : e.getStatut().name(),
            e.getNote() == null ? null : e.getNote().doubleValue(),
            e.getCommentaire(),
            e.getIntervieweur() == null ? null : e.getIntervieweur().getId(),
            intervieweurNom
        );
    }
}
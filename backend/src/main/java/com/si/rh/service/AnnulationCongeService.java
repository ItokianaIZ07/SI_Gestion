package com.si.rh.service;

import com.si.rh.dto.conge.AnnulationCongeRequest;
import com.si.rh.dto.conge.AnnulationCongeResponse;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.DemandeConge;
import com.si.rh.model.Employe;
import com.si.rh.model.MouvementConge;
import com.si.rh.model.RegleConge;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.enums.StatutCongeGlobal;
import com.si.rh.repository.DemandeCongeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.MouvementCongeRepository;
import com.si.rh.repository.RegleCongeRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.security.EmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AnnulationCongeService {

    private static final String ROLE_RH = "ROLE_RH";
    private static final String ROLE_EMPLOYE = "ROLE_EMPLOYE";
    private static final String TYPE_CONSOMMATION = "CONSOMMATION";
    private static final String TYPE_RESTITUTION = "RESTITUTION";
    private static final String TYPE_ANNULATION = "ANNULATION";

    private final DemandeCongeRepository demandeCongeRepository;
    private final MouvementCongeRepository mouvementCongeRepository;
    private final RegleCongeRepository regleCongeRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AnnulationCongeService(
            DemandeCongeRepository demandeCongeRepository,
            MouvementCongeRepository mouvementCongeRepository,
            RegleCongeRepository regleCongeRepository,
            EmployeRepository employeRepository,
            UtilisateurRepository utilisateurRepository) {
        this.demandeCongeRepository = demandeCongeRepository;
        this.mouvementCongeRepository = mouvementCongeRepository;
        this.regleCongeRepository = regleCongeRepository;
        this.employeRepository = employeRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public AnnulationCongeResponse annuler(
            Integer demandeId,
            AnnulationCongeRequest request,
            Authentication authentication) {
        DemandeConge demande = demandeCongeRepository.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Demande de congé introuvable : " + demandeId));

        if (demande.getStatutGlobal() != StatutCongeGlobal.VALIDEE) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Seule une demande de congé validée peut être annulée.");
        }

        Utilisateur utilisateur = getUtilisateurAutorise(authentication, demande.getEmploye());
        if (mouvementCongeRepository.existsByDemandeConge_IdAndTypeMouvement(
                demandeId, TYPE_ANNULATION)) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Un mouvement d'annulation existe déjà pour cette demande.");
        }
        if (mouvementCongeRepository.existsByDemandeConge_IdAndTypeMouvement(
                demandeId, TYPE_RESTITUTION)) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Un mouvement de restitution existe déjà pour cette demande.");
        }

        List<RegleConge> regles = regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(),
                demande.getDateDebut());
        if (regles.isEmpty()) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Aucune règle de congé active ne couvre la date de début demandée.");
        }

        LocalDateTime dateAnnulation = LocalDateTime.now();
        double joursRestitues = 0;
        if (regles.get(0).isDeductionSolde()) {
            joursRestitues = restituerSoldeSiNecessaire(
                    demande, utilisateur, request.motif().trim(), dateAnnulation.toLocalDate());
        }

        enregistrerAnnulation(demande, utilisateur, request.motif().trim());
        demande.setStatutGlobal(StatutCongeGlobal.ANNULEE);
        demandeCongeRepository.save(demande);

        return new AnnulationCongeResponse(
                demande.getId(),
                demande.getStatutGlobal().name(),
                joursRestitues,
                dateAnnulation,
                request.motif().trim());
    }

    private void enregistrerAnnulation(
            DemandeConge demande,
            Utilisateur utilisateur,
            String motif) {
        Employe employe = employeRepository.findByIdForUpdate(demande.getEmploye().getId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Employé introuvable pour la demande " + demande.getId()));

        MouvementConge annulation = new MouvementConge();
        annulation.setEmploye(employe);
        annulation.setTypeConge(demande.getTypeConge());
        annulation.setDemandeConge(demande);
        annulation.setUtilisateur(utilisateur);
        annulation.setTypeMouvement(TYPE_ANNULATION);
        annulation.setNombreJours(0);
        annulation.setPeriodeDebut(demande.getDateDebut());
        annulation.setMotif(motif);
        mouvementCongeRepository.save(annulation);
    }

    private double restituerSoldeSiNecessaire(
            DemandeConge demande,
            Utilisateur utilisateur,
            String motif,
            LocalDate aujourdhui) {
        List<MouvementConge> consommations =
                mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                        demande.getId(), TYPE_CONSOMMATION);
        if (consommations.size() > 1) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Plusieurs consommations existent pour cette demande; annulation impossible.");
        }

        List<MouvementConge> restitutions =
                mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                        demande.getId(), TYPE_RESTITUTION);
        if (!restitutions.isEmpty()) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Un mouvement de restitution existe déjà pour cette demande.");
        }
        if (consommations.isEmpty()) {
            return 0;
        }

        MouvementConge consommation = consommations.get(0);
        if (consommation.getNombreJours() >= 0) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Le mouvement de consommation de la demande est invalide.");
        }

        double joursDeduits = -consommation.getNombreJours();
        double joursUtilises;
        if (aujourdhui.isBefore(demande.getDateDebut())) {
            joursUtilises = 0;
        } else if (aujourdhui.isAfter(demande.getDateFin())) {
            joursUtilises = joursDeduits;
        } else {
            Integer joursOuvresEcoules = demandeCongeRepository.compterJoursOuvres(
                    demande.getDateDebut(), aujourdhui);
            if (joursOuvresEcoules == null || joursOuvresEcoules < 0) {
                throw new RegleMetierException(
                        HttpStatus.CONFLICT,
                        "Impossible de calculer les jours de congé déjà utilisés.");
            }
            joursUtilises = Math.min(joursDeduits, joursOuvresEcoules);
        }

        double joursARestituer = Math.max(0, joursDeduits - joursUtilises);
        if (joursARestituer == 0) {
            return 0;
        }

        Employe employe = employeRepository.findByIdForUpdate(demande.getEmploye().getId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Employé introuvable pour la demande " + demande.getId()));

        MouvementConge restitution = new MouvementConge();
        restitution.setEmploye(employe);
        restitution.setTypeConge(demande.getTypeConge());
        restitution.setDemandeConge(demande);
        restitution.setUtilisateur(utilisateur);
        restitution.setTypeMouvement(TYPE_RESTITUTION);
        restitution.setNombreJours(joursARestituer);
        restitution.setPeriodeDebut(demande.getDateDebut());
        restitution.setMotif("Restitution après annulation : " + motif);
        mouvementCongeRepository.save(restitution);
        return joursARestituer;
    }

    private Utilisateur getUtilisateurAutorise(
            Authentication authentication,
            Employe employeDemandeur) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw accesRefuse();
        }

        boolean rh = authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_RH.equals(authority.getAuthority()));
        if (rh) {
            return utilisateurRepository.findByEmail(authentication.getName())
                    .filter(utilisateur -> Boolean.TRUE.equals(utilisateur.getActif()))
                    .filter(utilisateur -> utilisateur.getRole() != null
                            && "RH".equals(utilisateur.getRole().getCode()))
                    .orElseThrow(AnnulationCongeService::accesRefuse);
        }

        boolean employe = authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_EMPLOYE.equals(authority.getAuthority()));
        if (employe
                && authentication.getPrincipal() instanceof EmployeePrincipal principal
                && Objects.equals(principal.id(), employeDemandeur.getId())) {
            Utilisateur utilisateur = employeDemandeur.getUtilisateur();
            if (utilisateur != null
                    && Boolean.TRUE.equals(utilisateur.getActif())
                    && utilisateur.getRole() != null
                    && "EMPLOYE".equals(utilisateur.getRole().getCode())) {
                return utilisateur;
            }
        }
        throw accesRefuse();
    }

    private static AccessDeniedException accesRefuse() {
        return new AccessDeniedException(
                "Seul l'employé concerné ou un utilisateur RH peut annuler cette demande.");
    }
}

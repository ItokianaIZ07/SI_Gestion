package com.si.rh.service;

import com.si.rh.dto.conge.DecisionCongeRequest;
import com.si.rh.dto.conge.DecisionCongeResponse;
import com.si.rh.dto.conge.DemandeCongeEnAttenteDTO;
import com.si.rh.dto.conge.ValidationCongeDTO;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Decision;
import com.si.rh.model.DemandeConge;
import com.si.rh.model.Employe;
import com.si.rh.model.MouvementConge;
import com.si.rh.model.RegleConge;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.ValidationConge;
import com.si.rh.model.enums.StatutCongeGlobal;
import com.si.rh.projection.VCongeDemandesProjection;
import com.si.rh.projection.VCongeValidationProjection;
import com.si.rh.repository.DecisionRepository;
import com.si.rh.repository.DemandeCongeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.MouvementCongeRepository;
import com.si.rh.repository.RegleCongeRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.repository.ValidationCongeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ValidationCongeService {

    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_RH = "RH";

    private final DemandeCongeRepository demandeCongeRepository;
    private final ValidationCongeRepository validationCongeRepository;
    private final DecisionRepository decisionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final RegleCongeRepository regleCongeRepository;
    private final MouvementCongeRepository mouvementCongeRepository;

    public ValidationCongeService(
            DemandeCongeRepository demandeCongeRepository,
            ValidationCongeRepository validationCongeRepository,
            DecisionRepository decisionRepository,
            UtilisateurRepository utilisateurRepository,
            EmployeRepository employeRepository,
            RegleCongeRepository regleCongeRepository,
            MouvementCongeRepository mouvementCongeRepository) {
        this.demandeCongeRepository = demandeCongeRepository;
        this.validationCongeRepository = validationCongeRepository;
        this.decisionRepository = decisionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.regleCongeRepository = regleCongeRepository;
        this.mouvementCongeRepository = mouvementCongeRepository;
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeEnAttenteDTO> getDemandesEnAttente(
            Authentication authentication) {
        Utilisateur utilisateur = getUtilisateur(authentication);
        String role = getRole(utilisateur);
        List<VCongeDemandesProjection> demandes;

        if (ROLE_MANAGER.equals(role)) {
            demandes = demandeCongeRepository.findDemandesEnAttenteManager(utilisateur.getId());
        } else if (ROLE_RH.equals(role)) {
            demandes = demandeCongeRepository.findDemandesEnAttenteRh();
        } else {
            throw accesRefuse();
        }

        Map<Integer, Employe> employes = employeRepository.findAllById(
                        demandes.stream()
                                .map(VCongeDemandesProjection::getIdEmploye)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(Employe::getId, employe -> employe));

        return demandes.stream()
                .map(demande -> {
                    Employe employe = employes.get(demande.getIdEmploye());
                    if (employe == null) {
                        throw new RessourceIntrouvableException(
                                "Employé introuvable pour la demande " + demande.getIdDemande());
                    }
                    return new DemandeCongeEnAttenteDTO(
                            demande.getIdDemande(),
                            employe.getId(),
                            employe.getNom(),
                            employe.getPrenom(),
                            employe.getMatricule(),
                            employe.getEmail(),
                            demande.getDateDebut(),
                            demande.getDateFin(),
                            demande.getTypeConge(),
                            demande.getNombreJours(),
                            demande.getMotif(),
                            demande.getStatutGlobal(),
                            demande.getDateDemande());
                })
                .toList();
    }

    @Transactional
    public DecisionCongeResponse decider(
            Integer demandeId,
            DecisionCongeRequest request,
            Authentication authentication) {
        Utilisateur utilisateur = getUtilisateur(authentication);
        String role = getRole(utilisateur);
        System.out.println("Role: "+role);
        DemandeConge demande = demandeCongeRepository.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Demande de congé introuvable : " + demandeId));

        int ordre;
        String etape;
        if (demande.getStatutGlobal() == StatutCongeGlobal.EN_ATTENTE_MANAGER) {
            ordre = 1;
            etape = ROLE_MANAGER;
            if (!ROLE_MANAGER.equals(role)) {
                throw accesRefuse();
            }
            verifierManagerDuDepartement(utilisateur, demande.getEmploye());
        } else if (demande.getStatutGlobal() == StatutCongeGlobal.EN_ATTENTE_RH) {
            ordre = 2;
            etape = ROLE_RH;
            if (!ROLE_RH.equals(role)) {
                throw accesRefuse();
            }
        } else {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Cette demande n'est plus en attente de validation.");
        }

        ValidationConge validation = validationCongeRepository
                .findByDemandeConge_IdAndOrdre(demandeId, ordre)
                .orElseThrow(() -> new RegleMetierException(
                        HttpStatus.CONFLICT,
                        "L'étape de validation attendue est absente."));
        if (!"EN_ATTENTE".equals(validation.getDecision().getCode())) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Cette étape a déjà été traitée.");
        }

        String codeDecision = request.decision();
        Decision decision = decisionRepository.findByCode(codeDecision)
                .orElseThrow(() -> new RegleMetierException(
                        HttpStatus.CONFLICT,
                        "La décision " + codeDecision + " est absente du référentiel."));
        LocalDateTime maintenant = LocalDateTime.now();

        validation.setUtilisateur(utilisateur);
        validation.setDecision(decision);
        validation.setCommentaire(request.commentaire());
        validation.setDateValidation(maintenant);
        validationCongeRepository.save(validation);

        if ("REFUSE".equals(codeDecision)) {
            demande.setStatutGlobal(StatutCongeGlobal.REFUSEE);
            demande.setDateDecisionFinale(maintenant);
        } else if (ordre == 1) {
            demande.setStatutGlobal(StatutCongeGlobal.EN_ATTENTE_RH);
        } else {
            approuverDefinitivement(demande, utilisateur);
            demande.setStatutGlobal(StatutCongeGlobal.VALIDEE);
            demande.setDateDecisionFinale(maintenant);
        }

        demandeCongeRepository.save(demande);
        return new DecisionCongeResponse(
                demande.getId(),
                etape,
                codeDecision,
                demande.getStatutGlobal().name(),
                request.commentaire(),
                maintenant);
    }

    @Transactional(readOnly = true)
    public List<ValidationCongeDTO> getHistorique(
            Integer demandeId,
            Authentication authentication) {
        Utilisateur utilisateur = getUtilisateur(authentication);
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Demande de congé introuvable : " + demandeId));
        String role = getRole(utilisateur);
        if (ROLE_MANAGER.equals(role)) {
            verifierManagerDuDepartement(utilisateur, demande.getEmploye());
        } else if (!ROLE_RH.equals(role)) {
            throw accesRefuse();
        }

        return demandeCongeRepository.findValidationsByDemandeId(demandeId)
                .stream()
                .map(this::toValidationDto)
                .toList();
    }

    private void approuverDefinitivement(DemandeConge demande, Utilisateur utilisateur) {
        Employe employe = employeRepository.findByIdForUpdate(demande.getEmploye().getId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Employé introuvable pour la demande " + demande.getId()));

        List<RegleConge> regles = regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(),
                demande.getDateDebut());
        if (regles.isEmpty()) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Aucune règle de congé active ne couvre la date de début demandée.");
        }
        if (!regles.get(0).isDeductionSolde()) {
            return;
        }

        if (mouvementCongeRepository.existsByDemandeConge_IdAndTypeMouvement(
                demande.getId(), "CONSOMMATION")) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Un mouvement de consommation existe déjà pour cette demande.");
        }

        double solde = demandeCongeRepository.getSoldeByEmpAndTypeConge(
                        employe.getId(),
                        demande.getTypeConge().getId())
                .orElseThrow(() -> new RegleMetierException(
                        HttpStatus.CONFLICT,
                        "Impossible de récupérer le solde de congé de cet employé."))
                .solde();
        if (demande.getNombreJours() > solde) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Solde insuffisant au moment de la validation. Solde disponible : "
                            + solde + " jours.");
        }

        MouvementConge mouvement = new MouvementConge();
        mouvement.setEmploye(employe);
        mouvement.setTypeConge(demande.getTypeConge());
        mouvement.setDemandeConge(demande);
        mouvement.setUtilisateur(utilisateur);
        mouvement.setTypeMouvement("CONSOMMATION");
        mouvement.setNombreJours(-demande.getNombreJours());
        mouvement.setMotif("Congé validé - demande " + demande.getId());
        mouvementCongeRepository.save(mouvement);
    }

    private Utilisateur getUtilisateur(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw accesRefuse();
        }
        return utilisateurRepository.findByEmail(authentication.getName())
                .filter(utilisateur -> Boolean.TRUE.equals(utilisateur.getActif()))
                .orElseThrow(ValidationCongeService::accesRefuse);
    }

    private String getRole(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null) {
            throw accesRefuse();
        }
        return utilisateur.getRole().getCode();
    }

    private void verifierManagerDuDepartement(Utilisateur utilisateur, Employe employe) {
        if (employe.getDepartement() == null
                || employe.getDepartement().getManager() == null
                || !Objects.equals(
                        employe.getDepartement().getManager().getId(),
                        utilisateur.getId())) {
            throw accesRefuse();
        }
    }

    private ValidationCongeDTO toValidationDto(VCongeValidationProjection validation) {
        return new ValidationCongeDTO(
                validation.getIdValidation(),
                validation.getIdDemandeConge(),
                validation.getIdUtilisateur(),
                validation.getEmailValidateur(),
                validation.getOrdre(),
                validation.getEtapeValidation(),
                validation.getCodeDecision(),
                validation.getCommentaire(),
                validation.getDateValidation());
    }

    private static AccessDeniedException accesRefuse() {
        return new AccessDeniedException(
                "Vous n'êtes pas autorisé à traiter cette demande de congé.");
    }
}

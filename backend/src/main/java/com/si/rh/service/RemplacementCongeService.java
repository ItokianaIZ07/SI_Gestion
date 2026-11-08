package com.si.rh.service;

import com.si.rh.dto.conge.DecisionRemplacementRequest;
import com.si.rh.dto.conge.DecisionRemplacementResponse;
import com.si.rh.dto.conge.RemplacementCongeResponse;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Employe;
import com.si.rh.model.RemplacementConge;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.RemplacementCongeRepository;
import com.si.rh.security.EmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RemplacementCongeService {

    private static final String ROLE_EMPLOYE = "ROLE_EMPLOYE";

    private final RemplacementCongeRepository remplacementCongeRepository;
    private final EmployeRepository employeRepository;

    public RemplacementCongeService(
            RemplacementCongeRepository remplacementCongeRepository,
            EmployeRepository employeRepository) {
        this.remplacementCongeRepository = remplacementCongeRepository;
        this.employeRepository = employeRepository;
    }

    @Transactional(readOnly = true)
    public List<RemplacementCongeResponse> getMesRemplacements(
            Authentication authentication) {
        Employe employe = getEmployeConnecte(authentication);
        return remplacementCongeRepository
                .findAllByEmployeRemplacant_IdOrderByDatePropositionDesc(employe.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DecisionRemplacementResponse decider(
            Long remplacementId,
            DecisionRemplacementRequest request,
            Authentication authentication) {
        Employe employe = getEmployeConnecte(authentication);
        RemplacementConge remplacement = remplacementCongeRepository
                .findByIdForUpdate(remplacementId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Proposition de remplacement introuvable : " + remplacementId));

        if (!employe.getId().equals(remplacement.getEmployeRemplacant().getId())) {
            throw accesRefuse();
        }
        if (!"EN_ATTENTE".equals(remplacement.getDecision())) {
            throw new RegleMetierException(
                    HttpStatus.CONFLICT,
                    "Cette proposition de remplacement a déjà été traitée.");
        }

        LocalDateTime dateDecision = LocalDateTime.now();
        remplacement.setDecision(request.decision());
        remplacement.setCommentaire(request.commentaire());
        remplacement.setDateDecision(dateDecision);
        remplacementCongeRepository.save(remplacement);

        return new DecisionRemplacementResponse(
                remplacement.getId(),
                remplacement.getDemandeConge().getId(),
                remplacement.getDecision(),
                remplacement.getCommentaire(),
                remplacement.getDateDecision());
    }

    private Employe getEmployeConnecte(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof EmployeePrincipal principal)
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> ROLE_EMPLOYE.equals(authority.getAuthority()))) {
            throw accesRefuse();
        }

        return employeRepository.findById(principal.id())
                .filter(employe -> employe.getStatut() == StatutEmploye.ACTIF)
                .orElseThrow(RemplacementCongeService::accesRefuse);
    }

    private RemplacementCongeResponse toResponse(RemplacementConge remplacement) {
        var demande = remplacement.getDemandeConge();
        var demandeur = demande.getEmploye();
        return new RemplacementCongeResponse(
                remplacement.getId(),
                demande.getId(),
                demandeur.getId(),
                demandeur.getNom(),
                demandeur.getPrenom(),
                demandeur.getMatricule(),
                demande.getTypeConge().getLibelle(),
                demande.getDateDebut(),
                demande.getDateFin(),
                demande.getNombreJours(),
                remplacement.getDateProposition(),
                remplacement.getDecision(),
                remplacement.getCommentaire(),
                remplacement.getDateDecision());
    }

    private static AccessDeniedException accesRefuse() {
        return new AccessDeniedException(
                "Seul le remplaçant actif connecté peut consulter ou décider cette proposition.");
    }
}

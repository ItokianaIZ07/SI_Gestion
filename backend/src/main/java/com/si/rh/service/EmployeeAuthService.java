package com.si.rh.service;

import com.si.rh.dto.EmployeeAuthResponse;
import com.si.rh.dto.EmployeeLoginRequest;
import com.si.rh.model.Employe;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.security.EmployeePrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class EmployeeAuthService {

    private static final String ROLE_EMPLOYE = "EMPLOYE";

    private final EmployeRepository employeRepository;

    public EmployeeAuthService(EmployeRepository employeRepository) {
        this.employeRepository = employeRepository;
    }

    @Transactional(readOnly = true)
    public EmployeePrincipal authentifier(EmployeeLoginRequest request) {
        Employe employe = employeRepository
                .findByMatriculeAndEmailAndNumeroCinAndStatut(
                        request.matricule(),
                        request.email(),
                        request.numeroCin(),
                        StatutEmploye.ACTIF)
                .orElseThrow(EmployeeAuthService::identifiantsInvalides);

        Utilisateur utilisateur = employe.getUtilisateur();
//        if (utilisateur == null
//                || !Boolean.TRUE.equals(utilisateur.getActif())
//                || !Objects.equals(utilisateur.getEmail(), employe.getEmail())
//                || utilisateur.getRole() == null
//                || !ROLE_EMPLOYE.equals(utilisateur.getRole().getCode())) {
//            throw identifiantsInvalides();
//        }

        return new EmployeePrincipal(
                employe.getId(),
                employe.getMatricule(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail());
    }

    public EmployeeAuthResponse creerReponse(EmployeePrincipal principal) {
        return new EmployeeAuthResponse(
                true,
                "Authentification réussie",
                ROLE_EMPLOYE,
                new EmployeeAuthResponse.EmployeeIdentity(
                        principal.id(),
                        principal.matricule(),
                        principal.nom(),
                        principal.prenom(),
                        principal.email()));
    }

    public EmployeeAuthResponse reponseEchec() {
        return new EmployeeAuthResponse(
                false,
                "Matricule, email ou numéro CIN incorrect",
                null,
                null);
    }

    private static BadCredentialsException identifiantsInvalides() {
        return new BadCredentialsException(
                "Matricule, email ou numéro CIN incorrect pour l'authentification");
    }
}

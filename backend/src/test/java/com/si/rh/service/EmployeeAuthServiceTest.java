package com.si.rh.service;

import com.si.rh.dto.EmployeeLoginRequest;
import com.si.rh.model.Employe;
import com.si.rh.model.Role;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.security.EmployeePrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.BadCredentialsException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAuthServiceTest {

    private static final EmployeeLoginRequest REQUEST =
            new EmployeeLoginRequest(
                    "jean@example.com", "EMP001", "101010101010");

    @Mock
    private EmployeRepository employeRepository;

    @InjectMocks
    private EmployeeAuthService employeeAuthService;

    @Test
    void authentifierRetourneLePrincipalEmployeLorsqueTousLesCriteresCorrespondent() {
        Employe employe = employe("EMPLOYE", true);
        when(employeRepository.findByMatriculeAndEmailAndNumeroCinAndStatut(
                "EMP001", "jean@example.com", "101010101010", StatutEmploye.ACTIF))
                .thenReturn(Optional.of(employe));

        EmployeePrincipal principal = employeeAuthService.authentifier(REQUEST);

        assertEquals(1, principal.id());
        assertEquals("EMP001", principal.matricule());
        assertEquals("ROLE_EMPLOYE",
                principal.getAuthorities().iterator().next().getAuthority());
        verify(employeRepository).findByMatriculeAndEmailAndNumeroCinAndStatut(
                "EMP001", "jean@example.com", "101010101010", StatutEmploye.ACTIF);
    }

    @Test
    void authentifierRejetteLesIdentifiantsSansCorrespondance() {
        when(employeRepository.findByMatriculeAndEmailAndNumeroCinAndStatut(
                "EMP001", "jean@example.com", "101010101010", StatutEmploye.ACTIF))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> employeeAuthService.authentifier(REQUEST));
    }

    @Test
    void authentifierRejetteUnCompteInactif() {
        Employe employe = employe("EMPLOYE", false);
        when(employeRepository.findByMatriculeAndEmailAndNumeroCinAndStatut(
                "EMP001", "jean@example.com", "101010101010", StatutEmploye.ACTIF))
                .thenReturn(Optional.of(employe));

        assertThrows(BadCredentialsException.class,
                () -> employeeAuthService.authentifier(REQUEST));
    }

    @Test
    void authentifierRejetteUnRoleAutreQueEmploye() {
        Employe employe = employe("RH", true);
        when(employeRepository.findByMatriculeAndEmailAndNumeroCinAndStatut(
                "EMP001", "jean@example.com", "101010101010", StatutEmploye.ACTIF))
                .thenReturn(Optional.of(employe));

        assertThrows(BadCredentialsException.class,
                () -> employeeAuthService.authentifier(REQUEST));
    }

    private Employe employe(String roleCode, boolean actif) {
        Role role = new Role();
        role.setCode(roleCode);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setActif(actif);
        utilisateur.setEmail("jean@example.com");
        utilisateur.setRole(role);

        Employe employe = new Employe();
        employe.setId(1);
        employe.setMatricule("EMP001");
        employe.setNom("Rakoto");
        employe.setPrenom("Jean");
        employe.setEmail("jean@example.com");
        employe.setUtilisateur(utilisateur);
        return employe;
    }
}

package com.si.rh.service;

import com.si.rh.dto.conge.DecisionCongeRequest;
import com.si.rh.dto.conge.DecisionCongeResponse;
import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.Decision;
import com.si.rh.model.Departement;
import com.si.rh.model.DemandeConge;
import com.si.rh.model.Employe;
import com.si.rh.model.MouvementConge;
import com.si.rh.model.RegleConge;
import com.si.rh.model.Role;
import com.si.rh.model.TypeConge;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.ValidationConge;
import com.si.rh.model.enums.StatutCongeGlobal;
import com.si.rh.repository.DecisionRepository;
import com.si.rh.repository.DemandeCongeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.MouvementCongeRepository;
import com.si.rh.repository.RegleCongeRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.repository.ValidationCongeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationCongeServiceTest {

    private static final Integer DEMANDE_ID = 24;
    private static final String EMAIL = "manager@example.test";

    @Mock
    private DemandeCongeRepository demandeCongeRepository;
    @Mock
    private ValidationCongeRepository validationCongeRepository;
    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private RegleCongeRepository regleCongeRepository;
    @Mock
    private MouvementCongeRepository mouvementCongeRepository;
    @Mock
    private Authentication authentication;

    private ValidationCongeService service;

    @BeforeEach
    void setUp() {
        service = new ValidationCongeService(
                demandeCongeRepository,
                validationCongeRepository,
                decisionRepository,
                utilisateurRepository,
                employeRepository,
                regleCongeRepository,
                mouvementCongeRepository);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(EMAIL);
    }

    @Test
    void managerAcceptationTransmetLaDemandeAuRh() {
        Utilisateur manager = utilisateur(7, "MANAGER");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_MANAGER, manager);
        ValidationConge validation = validation(demande, 1);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(manager));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 1))
                .thenReturn(Optional.of(validation));
        when(decisionRepository.findByCode("ACCEPTE"))
                .thenReturn(Optional.of(new Decision(2, "ACCEPTE", "Accepté")));

        DecisionCongeResponse response = service.decider(
                DEMANDE_ID, new DecisionCongeRequest("ACCEPTE", "Accord"), authentication);

        assertEquals("MANAGER", response.etapeValidee());
        assertEquals("EN_ATTENTE_RH", response.statutGlobal());
        assertEquals(StatutCongeGlobal.EN_ATTENTE_RH, demande.getStatutGlobal());
        assertEquals(manager, validation.getUtilisateur());
        verify(mouvementCongeRepository, never()).save(any());
    }

    @Test
    void refusManagerAnnuleLaDemandeSansConsommerLeSolde() {
        Utilisateur manager = utilisateur(7, "MANAGER");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_MANAGER, manager);
        ValidationConge validation = validation(demande, 1);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(manager));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 1))
                .thenReturn(Optional.of(validation));
        when(decisionRepository.findByCode("REFUSE"))
                .thenReturn(Optional.of(new Decision(3, "REFUSE", "Refusé")));

        DecisionCongeResponse response = service.decider(
                DEMANDE_ID, new DecisionCongeRequest("REFUSE", "Période indisponible"), authentication);

        assertEquals("ANNULEE", response.statutGlobal());
        assertEquals(StatutCongeGlobal.ANNULEE, demande.getStatutGlobal());
        assertEquals(manager, validation.getUtilisateur());
        assertEquals("REFUSE", validation.getDecision().getCode());
        assertEquals("Période indisponible", validation.getCommentaire());
        verify(validationCongeRepository).save(validation);
        verify(demandeCongeRepository).save(demande);
        verify(mouvementCongeRepository, never()).save(any());
    }

    @Test
    void refusRhAnnuleLaDemandeSansConsommerLeSolde() {
        Utilisateur rh = utilisateur(11, "RH");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_RH, rh);
        ValidationConge validation = validation(demande, 2);
        when(authentication.getName()).thenReturn("rh@example.test");
        when(utilisateurRepository.findByEmail("rh@example.test")).thenReturn(Optional.of(rh));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 2))
                .thenReturn(Optional.of(validation));
        when(decisionRepository.findByCode("REFUSE"))
                .thenReturn(Optional.of(new Decision(3, "REFUSE", "Refusé")));

        DecisionCongeResponse response = service.decider(
                DEMANDE_ID,
                new DecisionCongeRequest("REFUSE", "Solde insuffisant"),
                authentication);

        assertEquals("RH", response.etapeValidee());
        assertEquals("REFUSE", response.decision());
        assertEquals("ANNULEE", response.statutGlobal());
        assertEquals(StatutCongeGlobal.ANNULEE, demande.getStatutGlobal());
        assertEquals(rh, validation.getUtilisateur());
        assertEquals("REFUSE", validation.getDecision().getCode());
        assertEquals("Solde insuffisant", validation.getCommentaire());
        verify(validationCongeRepository).save(validation);
        verify(demandeCongeRepository).save(demande);
        verify(mouvementCongeRepository, never()).save(any());
    }

    @Test
    void managerNePeutPasValiderUnEmployeHorsDeSonDepartement() {
        Utilisateur manager = utilisateur(7, "MANAGER");
        Utilisateur autreManager = utilisateur(8, "MANAGER");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_MANAGER, autreManager);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(manager));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));

        assertThrows(AccessDeniedException.class, () -> service.decider(
                DEMANDE_ID, new DecisionCongeRequest("ACCEPTE", null), authentication));
        verify(validationCongeRepository, never())
                .save(any(ValidationConge.class));
    }

    @Test
    void rhAcceptationFinaleConsommeLeSoldeSelonLaRegleActive() {
        Utilisateur rh = utilisateur(11, "RH");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_RH, rh);
        ValidationConge validation = validation(demande, 2);
        Employe employe = demande.getEmploye();
        RegleConge regle = new RegleConge();
        regle.setDeductionSolde(true);
        when(authentication.getName()).thenReturn("rh@example.test");
        when(utilisateurRepository.findByEmail("rh@example.test")).thenReturn(Optional.of(rh));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 2))
                .thenReturn(Optional.of(validation));
        when(decisionRepository.findByCode("ACCEPTE"))
                .thenReturn(Optional.of(new Decision(2, "ACCEPTE", "Accepté")));
        when(employeRepository.findByIdForUpdate(employe.getId()))
                .thenReturn(Optional.of(employe));
        when(regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(), demande.getDateDebut()))
                .thenReturn(List.of(regle));
        when(demandeCongeRepository.getSoldeByEmpAndTypeConge(
                employe.getId(), demande.getTypeConge().getId()))
                .thenReturn(Optional.of(new SoldeCongeEmployeDTO(
                        employe.getId(), "Rakoto", "Jean", "EMP001",
                        "jean@example.test", "Annuel", 10)));

        DecisionCongeResponse response = service.decider(
                DEMANDE_ID, new DecisionCongeRequest("ACCEPTE", null), authentication);

        assertEquals("VALIDEE", response.statutGlobal());
        assertEquals(StatutCongeGlobal.VALIDEE, demande.getStatutGlobal());
        ArgumentCaptor<MouvementConge> mouvementCaptor =
                ArgumentCaptor.forClass(MouvementConge.class);
        verify(mouvementCongeRepository).save(mouvementCaptor.capture());
        assertEquals("CONSOMMATION", mouvementCaptor.getValue().getTypeMouvement());
        assertEquals(-5, mouvementCaptor.getValue().getNombreJours());
        assertEquals(demande, mouvementCaptor.getValue().getDemandeConge());
        assertEquals(employe, mouvementCaptor.getValue().getEmploye());
        assertEquals(demande.getTypeConge(), mouvementCaptor.getValue().getTypeConge());
        assertEquals(rh, mouvementCaptor.getValue().getUtilisateur());
    }

    @Test
    void rhNePeutPasDeciderAvantLaValidationManager() {
        Utilisateur rh = utilisateur(11, "RH");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_MANAGER, rh);
        when(authentication.getName()).thenReturn("rh@example.test");
        when(utilisateurRepository.findByEmail("rh@example.test")).thenReturn(Optional.of(rh));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));

        assertThrows(AccessDeniedException.class, () -> service.decider(
                DEMANDE_ID, new DecisionCongeRequest("REFUSE", null), authentication));
    }

    @Test
    void refusRhTermineLaDemandeSansConsommerLeSolde() {
        Utilisateur rh = utilisateur(11, "RH");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_RH, rh);
        when(authentication.getName()).thenReturn("rh@example.test");
        when(utilisateurRepository.findByEmail("rh@example.test")).thenReturn(Optional.of(rh));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 2))
                .thenReturn(Optional.of(validation(demande, 2)));
        when(decisionRepository.findByCode("REFUSE"))
                .thenReturn(Optional.of(new Decision(3, "REFUSE", "Refusé")));

        DecisionCongeResponse response = service.decider(
                DEMANDE_ID, new DecisionCongeRequest("REFUSE", "Motif RH"), authentication);

        assertEquals("REFUSEE", response.statutGlobal());
        assertEquals(StatutCongeGlobal.REFUSEE, demande.getStatutGlobal());
        verify(mouvementCongeRepository, never()).save(any());
    }

    @Test
    void leSoldeEstRecontroleAvantLapprobationFinale() {
        Utilisateur rh = utilisateur(11, "RH");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_RH, rh);
        when(authentication.getName()).thenReturn("rh@example.test");
        when(utilisateurRepository.findByEmail("rh@example.test")).thenReturn(Optional.of(rh));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 2))
                .thenReturn(Optional.of(validation(demande, 2)));
        when(decisionRepository.findByCode("ACCEPTE"))
                .thenReturn(Optional.of(new Decision(2, "ACCEPTE", "Accepté")));
        when(employeRepository.findByIdForUpdate(demande.getEmploye().getId()))
                .thenReturn(Optional.of(demande.getEmploye()));
        RegleConge regle = new RegleConge();
        regle.setDeductionSolde(true);
        when(regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(), demande.getDateDebut()))
                .thenReturn(List.of(regle));
        when(demandeCongeRepository.getSoldeByEmpAndTypeConge(
                demande.getEmploye().getId(), demande.getTypeConge().getId()))
                .thenReturn(Optional.of(new SoldeCongeEmployeDTO(
                        3, "Rakoto", "Jean", "EMP001", "jean@example.test", "Annuel", 2)));

        assertThrows(RegleMetierException.class, () -> service.decider(
                DEMANDE_ID, new DecisionCongeRequest("ACCEPTE", null), authentication));
        verify(mouvementCongeRepository, never()).save(any());
    }

    @Test
    void rhNeCreePasDeDeuxiemeConsommationPourLaMemeDemande() {
        Utilisateur rh = utilisateur(11, "RH");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_RH, rh);
        when(authentication.getName()).thenReturn("rh@example.test");
        when(utilisateurRepository.findByEmail("rh@example.test")).thenReturn(Optional.of(rh));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 2))
                .thenReturn(Optional.of(validation(demande, 2)));
        when(decisionRepository.findByCode("ACCEPTE"))
                .thenReturn(Optional.of(new Decision(2, "ACCEPTE", "Accepté")));
        when(employeRepository.findByIdForUpdate(demande.getEmploye().getId()))
                .thenReturn(Optional.of(demande.getEmploye()));
        RegleConge regle = new RegleConge();
        regle.setDeductionSolde(true);
        when(regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(), demande.getDateDebut()))
                .thenReturn(List.of(regle));
        when(mouvementCongeRepository.existsByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION"))
                .thenReturn(true);

        assertThrows(RegleMetierException.class, () -> service.decider(
                DEMANDE_ID, new DecisionCongeRequest("ACCEPTE", null), authentication));
        verify(mouvementCongeRepository, never()).save(any());
        verify(demandeCongeRepository, never()).getSoldeByEmpAndTypeConge(
                demande.getEmploye().getId(), demande.getTypeConge().getId());
    }

    @Test
    void rhNinscritPasDeConsommationSiLaRegleNeDeduitPasLeSolde() {
        Utilisateur rh = utilisateur(11, "RH");
        DemandeConge demande = demande(StatutCongeGlobal.EN_ATTENTE_RH, rh);
        when(authentication.getName()).thenReturn("rh@example.test");
        when(utilisateurRepository.findByEmail("rh@example.test")).thenReturn(Optional.of(rh));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(validationCongeRepository.findByDemandeConge_IdAndOrdre(DEMANDE_ID, 2))
                .thenReturn(Optional.of(validation(demande, 2)));
        when(decisionRepository.findByCode("ACCEPTE"))
                .thenReturn(Optional.of(new Decision(2, "ACCEPTE", "Accepté")));
        when(employeRepository.findByIdForUpdate(demande.getEmploye().getId()))
                .thenReturn(Optional.of(demande.getEmploye()));
        RegleConge regle = new RegleConge();
        regle.setDeductionSolde(false);
        when(regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(), demande.getDateDebut()))
                .thenReturn(List.of(regle));

        DecisionCongeResponse response = service.decider(
                DEMANDE_ID, new DecisionCongeRequest("ACCEPTE", null), authentication);

        assertEquals("VALIDEE", response.statutGlobal());
        verify(mouvementCongeRepository, never()).existsByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION");
        verify(mouvementCongeRepository, never()).save(any());
    }

    private Utilisateur utilisateur(Integer id, String roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setEmail(roleCode.equals("RH") ? "rh@example.test" : EMAIL);
        utilisateur.setActif(true);
        utilisateur.setRole(role);
        return utilisateur;
    }

    private DemandeConge demande(StatutCongeGlobal statut, Utilisateur manager) {
        Employe employe = new Employe();
        employe.setId(3);
        employe.setMatricule("EMP001");
        employe.setNom("Rakoto");
        employe.setPrenom("Jean");
        employe.setEmail("jean@example.test");

        Departement departement = new Departement();
        departement.setManager(manager);
        employe.setDepartement(departement);

        TypeConge typeConge = new TypeConge();
        typeConge.setId(5);

        DemandeConge demande = new DemandeConge();
        demande.setId(DEMANDE_ID);
        demande.setEmploye(employe);
        demande.setTypeConge(typeConge);
        demande.setDateDebut(LocalDate.of(2026, 12, 1));
        demande.setDateFin(LocalDate.of(2026, 12, 5));
        demande.setNombreJours(5);
        demande.setStatutGlobal(statut);
        return demande;
    }

    private ValidationConge validation(DemandeConge demande, int ordre) {
        ValidationConge validation = new ValidationConge();
        validation.setDemandeConge(demande);
        validation.setOrdre(ordre);
        validation.setDecision(new Decision(1, "EN_ATTENTE", "En attente"));
        return validation;
    }
}

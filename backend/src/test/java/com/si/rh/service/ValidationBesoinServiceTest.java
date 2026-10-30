package com.si.rh.service;

import com.si.rh.dto.BesoinValidationDTO;
import com.si.rh.model.Besoin;
import com.si.rh.model.Decision;
import com.si.rh.model.Departement;
import com.si.rh.model.EtatValidation;
import com.si.rh.model.Poste;
import com.si.rh.model.Role;
import com.si.rh.model.TypeBesoin;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.ValidationBesoin;
import com.si.rh.model.enums.Priorite;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.RecrutementRepository;
import com.si.rh.repository.ValidationBesoinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationBesoinServiceTest {
    private static final Integer ID_BESOIN = 42;
    private static final String EMAIL = "validateur@example.test";

    @Mock
    private ValidationBesoinRepository validationBesoinRepository;
    @Mock
    private BesoinRepository besoinRepository;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private RecrutementRepository recrutementRepository;
    @Mock
    private DecisionService decisionService;
    @Mock
    private EtatValidationService etatValidationService;
    @Mock
    private UtilisateurDetailsService utilisateurDetailsService;
    @Mock
    private NotificationService notificationService;

    private ValidationBesoinService service;

    @BeforeEach
    void setUp() {
        service = new ValidationBesoinService(
                validationBesoinRepository,
                besoinRepository,
                decisionService,
                etatValidationService,
                utilisateurDetailsService,
                null,
                employeRepository,
                null,
                recrutementRepository,
                notificationService);
    }

    @Test
    void financeAndDirectionCannotDecideWhileRhIsWaiting() {
        when(validationBesoinRepository.findAllForBesoins(anyCollection())).thenReturn(List.of());

        assertTrue(service.etapeEnAttente(ID_BESOIN, "RH"));
        assertFalse(service.peutPrendreDecision(ID_BESOIN, "FINANCE"));
        assertFalse(service.peutPrendreDecision(ID_BESOIN, "DIRECTION"));
    }

    @Test
    void financeAndDirectionCanDecideAfterRhAccepts() {
        useHistory(validation("RH", "ACCEPTE", 1, "2026-10-22T09:00:00Z"));

        assertTrue(service.peutPrendreDecision(ID_BESOIN, "FINANCE"));
        assertTrue(service.peutPrendreDecision(ID_BESOIN, "DIRECTION"));
    }

    @Test
    void rhRefusalImmediatelyRefusesAndBlocksOtherStages() {
        useHistory(validation("RH", "REFUSE", 1, "2026-10-22T09:00:00Z"));

        assertEquals("REFUSE", service.getStatutGlobal(ID_BESOIN));
        assertTrue(service.besoinRefuse(ID_BESOIN));
        assertFalse(service.peutPrendreDecision(ID_BESOIN, "FINANCE"));
        assertFalse(service.peutPrendreDecision(ID_BESOIN, "DIRECTION"));
    }

    @Test
    void financeRefusalImmediatelyRefusesAndBlocksDirection() {
        useHistory(
                validation("RH", "ACCEPTE", 1, "2026-10-22T09:00:00Z"),
                validation("FINANCE", "REFUSE", 2, "2026-10-22T10:00:00Z"));

        assertEquals("REFUSE", service.getStatutGlobal(ID_BESOIN));
        assertFalse(service.peutPrendreDecision(ID_BESOIN, "DIRECTION"));
    }

    @Test
    void directionRefusalImmediatelyRefusesTheNeed() {
        useHistory(
                validation("RH", "ACCEPTE", 1, "2026-10-22T09:00:00Z"),
                validation("FINANCE", "ACCEPTE", 2, "2026-10-22T10:00:00Z"),
                validation("DIRECTION", "REFUSE", 3, "2026-10-22T11:00:00Z"));

        assertEquals("REFUSE", service.getStatutGlobal(ID_BESOIN));
        assertTrue(service.besoinRefuse(ID_BESOIN));
    }

    @Test
    void allThreeAcceptancesProduceAcceptedGlobalStatus() {
        useHistory(
                validation("RH", "ACCEPTE", 1, "2026-10-22T09:00:00Z"),
                validation("FINANCE", "ACCEPTE", 2, "2026-10-22T10:00:00Z"),
                validation("DIRECTION", "ACCEPTE", 3, "2026-10-22T11:00:00Z"));

        assertEquals("ACCEPTE", service.getStatutGlobal(ID_BESOIN));
        assertTrue(service.toutesEtapesAcceptees(ID_BESOIN));
    }

    @Test
    void anAlreadyTreatedStageHasDisabledDecisionButtons() {
        useHistory(
                validation("RH", "ACCEPTE", 1, "2026-10-22T09:00:00Z"),
                validation("FINANCE", "ACCEPTE", 2, "2026-10-22T10:00:00Z"));
        when(utilisateurDetailsService.getByEmail(EMAIL)).thenReturn(user("FINANCE"));
        when(besoinRepository.findAll()).thenReturn(List.of(besoin()));

        BesoinValidationDTO dto = service.getBesoinsAvecEtat(EMAIL).get(0);

        assertFalse(dto.peutDecider());
        assertTrue(dto.accepterDesactive());
        assertTrue(dto.refuserDesactive());
    }

    @Test
    void userWithoutAnAuthorizedRoleIsRejectedByTheDecisionService() {
        when(utilisateurDetailsService.getByEmail(EMAIL)).thenReturn(user("MANAGER"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.prendreDecision(ID_BESOIN, "ACCEPTE", null, null, null, EMAIL));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(validationBesoinRepository, times(0)).saveAndFlush(any(ValidationBesoin.class));
    }

    @Test
    void acceptedDecisionIsSavedForAuthenticatedRoleAndUpdatesPendingStatus() {
        Utilisateur validateur = user("FINANCE");
        Besoin besoin = besoin();
        Decision decision = new Decision(2, "ACCEPTE", "Acceptée");
        EtatValidation etat = new EtatValidation(2, "FINANCE", "Finance", 2);
        when(utilisateurDetailsService.getByEmail(EMAIL)).thenReturn(validateur);
        when(besoinRepository.findByIdForUpdate(ID_BESOIN)).thenReturn(Optional.of(besoin));
        when(validationBesoinRepository.findAllForBesoins(anyCollection()))
                .thenReturn(List.of(validation("RH", "ACCEPTE", 1, "2026-10-22T09:00:00Z")));
        when(decisionService.getByCode("ACCEPTE")).thenReturn(decision);
        when(etatValidationService.getByCode("FINANCE")).thenReturn(etat);
        when(validationBesoinRepository.saveAndFlush(any(ValidationBesoin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(besoinRepository.save(any(Besoin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ValidationBesoin saved = service.prendreDecision(ID_BESOIN, "accepte", "Budget validé", null, null, EMAIL);

        assertEquals(validateur, saved.getValidateur());
        assertEquals("FINANCE", saved.getEtatValidation().getCode());
        assertEquals("ACCEPTE", saved.getDecision().getCode());
        assertEquals(StatutGlobalBesoin.EN_ATTENTE_DIRECTION, besoin.getStatutGlobal());
        verify(validationBesoinRepository).saveAndFlush(saved);
        verify(notificationService).notifierChangementEtatBesoin(besoin, "FINANCE", "ACCEPTE");
        verify(notificationService).notifierEtapeSuivante(besoin, "FINANCE");
    }

    @Test
    void latestDatedDecisionWinsOverOlderHistoryAndPendingRows() {
        ValidationBesoin oldRefusal = validation("RH", "REFUSE", 1, "2026-10-20T09:00:00Z");
        ValidationBesoin latestAcceptance = validation("RH", "ACCEPTE", 2, "2026-10-21T09:00:00Z");
        ValidationBesoin undatedPending = validation("RH", "EN_ATTENTE", 3, null);
        useHistory(oldRefusal, latestAcceptance, undatedPending);

        assertTrue(service.rhAccepte(ID_BESOIN));
        assertEquals("EN_ATTENTE", service.getStatutGlobal(ID_BESOIN));
        assertTrue(service.peutPrendreDecision(ID_BESOIN, "FINANCE"));
    }

    private void useHistory(ValidationBesoin... validations) {
        when(validationBesoinRepository.findAllForBesoins(anyCollection()))
                .thenReturn(List.of(validations));
    }

    private ValidationBesoin validation(String role, String decision, int id, String dateDecision) {
        ValidationBesoin validation = new ValidationBesoin();
        validation.setId(id);
        validation.setBesoin(besoin());
        validation.setEtatValidation(new EtatValidation(id, role, role, id));
        validation.setDecision(new Decision(id, decision, decision));
        validation.setDateDecision(dateDecision == null ? null : OffsetDateTime.parse(dateDecision));
        return validation;
    }

    private Utilisateur user(String roleCode) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(EMAIL);
        utilisateur.setRole(new Role(1, roleCode, roleCode));
        return utilisateur;
    }

    private Besoin besoin() {
        Departement departement = new Departement();
        departement.setNom("IT");
        Poste poste = new Poste();
        poste.setNom("Développeur");
        poste.setDepartement(departement);

        Utilisateur demandeur = new Utilisateur();
        demandeur.setNom("Nom");
        demandeur.setPrenom("Prénom");

        Besoin besoin = new Besoin();
        besoin.setId(ID_BESOIN);
        besoin.setPoste(poste);
        besoin.setDemandeur(demandeur);
        besoin.setTypeBesoin(new TypeBesoin(1, "RENFORT", "Renfort"));
        besoin.setEffectif(1);
        besoin.setPriorite(Priorite.NORMALE);
        besoin.setSalaireEstime(BigDecimal.valueOf(1000));
        return besoin;
    }
}

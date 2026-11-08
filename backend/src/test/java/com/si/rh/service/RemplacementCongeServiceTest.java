package com.si.rh.service;

import com.si.rh.dto.conge.DecisionRemplacementRequest;
import com.si.rh.dto.conge.DecisionRemplacementResponse;
import com.si.rh.dto.conge.RemplacementCongeResponse;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.DemandeConge;
import com.si.rh.model.Employe;
import com.si.rh.model.RemplacementConge;
import com.si.rh.model.TypeConge;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.RemplacementCongeRepository;
import com.si.rh.security.EmployeePrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemplacementCongeServiceTest {

    private static final Integer REMPLACANT_ID = 8;

    private final RemplacementCongeRepository remplacementRepository =
            mock(RemplacementCongeRepository.class);
    private final EmployeRepository employeRepository = mock(EmployeRepository.class);
    private final Authentication authentication = mock(Authentication.class);

    private final RemplacementCongeService service =
            new RemplacementCongeService(remplacementRepository, employeRepository);

    private Employe remplacant;

    @BeforeEach
    void setUp() {
        remplacant = employe(REMPLACANT_ID, StatutEmploye.ACTIF);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new EmployeePrincipal(
                REMPLACANT_ID, "EMP008", "Remplaçant", "Test", "remplacant@example.test"));
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE")))
                .when(authentication).getAuthorities();
        when(employeRepository.findById(REMPLACANT_ID)).thenReturn(Optional.of(remplacant));
    }

    @Test
    void retourneSeulementLesRemplacementsDeLEmployeConnecte() {
        RemplacementConge remplacement = remplacement(remplacant, "EN_ATTENTE");
        when(remplacementRepository
                .findAllByEmployeRemplacant_IdOrderByDatePropositionDesc(REMPLACANT_ID))
                .thenReturn(List.of(remplacement));

        List<RemplacementCongeResponse> response =
                service.getMesRemplacements(authentication);

        assertEquals(1, response.size());
        assertEquals(remplacement.getId(), response.get(0).idRemplacement());
        assertEquals(12, response.get(0).idDemande());
        assertEquals("Congé annuel", response.get(0).typeConge());
        assertEquals("EN_ATTENTE", response.get(0).decision());
        verify(remplacementRepository)
                .findAllByEmployeRemplacant_IdOrderByDatePropositionDesc(REMPLACANT_ID);
    }

    @Test
    void accepteUnePropositionEtEnregistreCommentaireEtDate() {
        RemplacementConge remplacement = remplacement(remplacant, "EN_ATTENTE");
        when(remplacementRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(remplacement));

        DecisionRemplacementResponse response = service.decider(
                4L,
                new DecisionRemplacementRequest("ACCEPTE", "Je peux assurer le remplacement."),
                authentication);

        assertEquals("ACCEPTE", response.decision());
        assertEquals("Je peux assurer le remplacement.", response.commentaire());
        assertEquals("ACCEPTE", remplacement.getDecision());
        assertEquals("Je peux assurer le remplacement.", remplacement.getCommentaire());
        assertEquals(response.dateDecision(), remplacement.getDateDecision());
        verify(remplacementRepository).save(remplacement);
    }

    @Test
    void refuseUnePropositionEtInterditDeTraiterDeuxFois() {
        RemplacementConge remplacement = remplacement(remplacant, "EN_ATTENTE");
        when(remplacementRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(remplacement));

        service.decider(4L, new DecisionRemplacementRequest("REFUSE", "Indisponible"), authentication);
        assertEquals("REFUSE", remplacement.getDecision());

        when(remplacementRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(remplacement));
        assertThrows(RegleMetierException.class, () -> service.decider(
                5L, new DecisionRemplacementRequest("ACCEPTE", null), authentication));
    }

    @Test
    void refuseLaDecisionSiLeRemplacantConnecteNestPasLeDestinataire() {
        RemplacementConge remplacement = remplacement(remplacant, "EN_ATTENTE");
        when(authentication.getPrincipal()).thenReturn(new EmployeePrincipal(
                99, "EMP099", "Autre", "Employé", "autre@example.test"));
        when(employeRepository.findById(99)).thenReturn(Optional.of(
                employe(99, StatutEmploye.ACTIF)));
        when(remplacementRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(remplacement));

        assertThrows(AccessDeniedException.class, () -> service.decider(
                4L, new DecisionRemplacementRequest("ACCEPTE", null), authentication));
        verify(remplacementRepository, never()).save(remplacement);
    }

    private RemplacementConge remplacement(Employe remplacant, String decision) {
        Employe demandeur = employe(3, StatutEmploye.ACTIF);
        TypeConge typeConge = new TypeConge(1, "ANNUEL", "Congé annuel", null, true);
        DemandeConge demande = new DemandeConge();
        demande.setId(12);
        demande.setEmploye(demandeur);
        demande.setTypeConge(typeConge);
        demande.setDateDebut(LocalDate.of(2026, 12, 1));
        demande.setDateFin(LocalDate.of(2026, 12, 5));
        demande.setNombreJours(5);

        RemplacementConge remplacement = new RemplacementConge();
        remplacement.setId(4L);
        remplacement.setDemandeConge(demande);
        remplacement.setEmployeRemplacant(remplacant);
        remplacement.setDecision(decision);
        remplacement.setDateProposition(LocalDateTime.of(2026, 11, 8, 10, 0));
        return remplacement;
    }

    private Employe employe(Integer id, StatutEmploye statut) {
        Employe employe = new Employe();
        employe.setId(id);
        employe.setMatricule("EMP" + id);
        employe.setNom("Employé");
        employe.setPrenom("Test");
        employe.setEmail("employe" + id + "@example.test");
        employe.setStatut(statut);
        return employe;
    }
}

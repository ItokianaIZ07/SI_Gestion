package com.si.rh.service;

import com.si.rh.dto.conge.DemandeCongeRequest;
import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.model.Decision;
import com.si.rh.model.Departement;
import com.si.rh.model.DemandeConge;
import com.si.rh.model.Employe;
import com.si.rh.model.RegleConge;
import com.si.rh.model.TypeConge;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.DecisionRepository;
import com.si.rh.repository.DemandeCongeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.RegleCongeRepository;
import com.si.rh.repository.RemplacementCongeRepository;
import com.si.rh.repository.TypeCongeRepository;
import com.si.rh.repository.ValidationCongeRepository;
import com.si.rh.model.RemplacementConge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DemandeCongeRemplacementTest {

    @Mock
    private DemandeCongeRepository demandeCongeRepository;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private TypeCongeRepository typeCongeRepository;
    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private ValidationCongeRepository validationCongeRepository;
    @Mock
    private RegleCongeRepository regleCongeRepository;
    @Mock
    private RemplacementCongeRepository remplacementCongeRepository;

    @InjectMocks
    private DemandeCongeService service;

    private Employe demandeur;
    private Employe remplacant;
    private TypeConge typeConge;
    private RegleConge regle;

    @BeforeEach
    void setUp() {
        Departement departement = new Departement();
        departement.setId(5);

        demandeur = employe(10, "EMP010", departement);
        remplacant = employe(11, "EMP011", departement);
        typeConge = new TypeConge(3, "ANNUEL", "Congé annuel", null, true);
        regle = new RegleConge();
        regle.setTypeConge(typeConge);
        regle.setRemplacantObligatoire(true);
        regle.setActif(true);

        when(decisionRepository.findByCode("EN_ATTENTE"))
                .thenReturn(Optional.of(new Decision(1, "EN_ATTENTE", "En attente")));
        when(typeCongeRepository.findById(3)).thenReturn(Optional.of(typeConge));
        when(employeRepository.findById(10)).thenReturn(Optional.of(demandeur));
        when(regleCongeRepository.findApplicableRules(3, LocalDate.of(2026, 12, 1)))
                .thenReturn(List.of(regle));
    }

    @Test
    void exigeUnRemplacantQuandLaRegleLeRendObligatoire() {
        DemandeCongeRequest request = request(null);

        assertThrows(IllegalArgumentException.class, () -> service.save(request));

        verify(demandeCongeRepository, never()).save(any(DemandeConge.class));
        verify(remplacementCongeRepository, never()).save(any(RemplacementConge.class));
    }

    @Test
    void enregistreLeRemplacantChoisiDansLaTableDeRemplacement() {
        when(employeRepository.findById(11)).thenReturn(Optional.of(remplacant));
        when(demandeCongeRepository.getSoldeByEmpAndTypeConge(10, 3))
                .thenReturn(Optional.of(new SoldeCongeEmployeDTO(
                        10, "Demandeur", "Employé", "EMP010",
                        "employe@example.test", "Congé annuel", 10)));
        when(demandeCongeRepository.save(any(DemandeConge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DemandeConge saved = service.save(request(11));

        ArgumentCaptor<RemplacementConge> captor =
                ArgumentCaptor.forClass(RemplacementConge.class);
        verify(remplacementCongeRepository).save(captor.capture());
        assertEquals(saved, captor.getValue().getDemandeConge());
        assertEquals(remplacant, captor.getValue().getEmployeRemplacant());
        assertEquals("EN_ATTENTE", captor.getValue().getDecision());
    }

    @Test
    void refuseUnRemplacantHorsDuDepartementDuDemandeur() {
        Departement autreDepartement = new Departement();
        autreDepartement.setId(6);
        Employe remplacantAutreDepartement = employe(11, "EMP011", autreDepartement);
        when(employeRepository.findById(11))
                .thenReturn(Optional.of(remplacantAutreDepartement));

        assertThrows(IllegalArgumentException.class, () -> service.save(request(11)));

        verify(demandeCongeRepository, never()).save(any(DemandeConge.class));
        verify(remplacementCongeRepository, never()).save(any(RemplacementConge.class));
    }

    private DemandeCongeRequest request(Integer idRemplacant) {
        return new DemandeCongeRequest(
                10,
                3,
                idRemplacant,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                5,
                "Congé annuel");
    }

    private Employe employe(Integer id, String matricule, Departement departement) {
        Employe employe = new Employe();
        employe.setId(id);
        employe.setMatricule(matricule);
        employe.setNom("Employé");
        employe.setPrenom("Test");
        employe.setEmail(matricule.toLowerCase() + "@example.test");
        employe.setDepartement(departement);
        employe.setStatut(StatutEmploye.ACTIF);
        return employe;
    }
}

package com.si.rh.service;

import com.si.rh.dto.BudgetResponse;
import com.si.rh.dto.ManagerDashboardResponse;
import com.si.rh.dto.VisionDepartementResponse;
import com.si.rh.model.Departement;
import com.si.rh.model.Utilisateur;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDashboardServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private DepartementRepository departementRepository;

    @Mock
    private DepartementService departementService;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private ManagerDashboardService managerDashboardService;

    @Test
    void returnsTheAuthenticatedManagersDepartmentAndItsBudget() {
        Utilisateur manager = new Utilisateur();
        manager.setId(51);
        Departement departement = new Departement();
        departement.setId(13);

        when(utilisateurRepository.findByEmail("manager@example.test")).thenReturn(Optional.of(manager));
        when(departementRepository.findFirstByManager_IdOrderByNomAsc(51))
                .thenReturn(Optional.of(departement));
        when(departementService.getDepartement(13)).thenReturn(new VisionDepartementResponse(
                13, "Informatique", 100, 7, 1, 2, 5, 1,
                Map.of("EN_ATTENTE_RH", 2L),
                List.of(new VisionDepartementResponse.DemandeResume(
                        201, "Développeur Java", 2, "EN_ATTENTE_RH", null
                ))
        ));
        int annee = LocalDate.now().getYear();
        when(budgetService.calculer(annee)).thenReturn(new BudgetResponse(
                new BudgetResponse.Total(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                ),
                List.of(
                        new BudgetResponse.LigneDepartement(
                                13, "Informatique", new BigDecimal("1000"), new BigDecimal("300"),
                                new BigDecimal("200"), new BigDecimal("500")
                        ),
                        new BudgetResponse.LigneDepartement(
                                14, "Commercial", new BigDecimal("9000"), BigDecimal.ZERO,
                                BigDecimal.ZERO, new BigDecimal("9000")
                        )
                )
        ));

        ManagerDashboardResponse response = managerDashboardService.getDashboard("manager@example.test");

        assertEquals(13, response.departementId());
        assertEquals("Informatique", response.nomDepartement());
        assertEquals(2L, response.demandesParStatut().get("EN_ATTENTE_RH"));
        assertEquals(1, response.demandes().size());
        assertEquals(201, response.demandes().get(0).id());
        assertEquals(new BigDecimal("1000"), response.budget().montant());
        assertEquals(new BigDecimal("500"), response.budget().restant());
        assertEquals(annee, response.budget().annee());
        verify(departementRepository).findFirstByManager_IdOrderByNomAsc(51);
    }
}

package com.si.rh.service;

import com.si.rh.dto.BudgetResponse;
import com.si.rh.dto.ManagerDashboardResponse;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class ManagerDashboardService {

    private final UtilisateurRepository utilisateurRepository;
    private final DepartementRepository departementRepository;
    private final DepartementService departementService;
    private final BudgetService budgetService;

    public ManagerDashboardService(
            UtilisateurRepository utilisateurRepository,
            DepartementRepository departementRepository,
            DepartementService departementService,
            BudgetService budgetService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.departementRepository = departementRepository;
        this.departementService = departementService;
        this.budgetService = budgetService;
    }

    public ManagerDashboardResponse getDashboard(String emailManager) {
        var manager = utilisateurRepository.findByEmail(emailManager)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur connecté introuvable"));
        var departement = departementRepository.findFirstByManager_IdOrderByNomAsc(manager.getId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun département n'est associé à ce manager"
                ));

        var vision = departementService.getDepartement(departement.getId());
        int annee = LocalDate.now().getYear();
        BudgetResponse.LigneDepartement budgetDepartement = budgetService.calculer(annee)
                .departements()
                .stream()
                .filter(ligne -> ligne.idDepartement().equals(departement.getId()))
                .findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Budget du département introuvable"
                ));

        var budget = new ManagerDashboardResponse.Budget(
                annee,
                budgetDepartement.budget(),
                budgetDepartement.masseSalariale(),
                budgetDepartement.depenses(),
                budgetDepartement.restant()
        );

        return new ManagerDashboardResponse(
                vision.id(),
                vision.nom(),
                vision.effectifMax(),
                vision.effectifActif(),
                vision.effectifSuspendu(),
                vision.effectifParti(),
                vision.postesActifs(),
                vision.postesInactifs(),
                vision.demandesParStatut(),
                vision.demandes(),
                budget
        );
    }
}

package com.si.rh.service;

// BY : Fenosoa
// Calcul automatique du budget annuel :
//   restant = budget alloué - masse salariale (contrats) - dépenses internes (depense_departement).

import com.si.rh.dto.BudgetResponse;
import com.si.rh.dto.BudgetResponse.LigneDepartement;
import com.si.rh.dto.BudgetResponse.Total;
import com.si.rh.dto.LigneSalaire;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.Departement;
import com.si.rh.repository.BudgetDepartementRepository;
import com.si.rh.repository.ContratRepository;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.DepenseDepartementRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {

    /** Les contrats en brouillon ou en attente de signature ne coûtent encore rien. */
    private static final List<String> STATUTS_CONTRAT_COMPTES = List.of("ACTIF", "TERMINE", "RESILIE");

    private final DepartementRepository departementRepository;
    private final BudgetDepartementRepository budgetRepository;
    private final DepenseDepartementRepository depenseRepository;
    private final ContratRepository contratRepository;

    public BudgetService(DepartementRepository departementRepository,
                         BudgetDepartementRepository budgetRepository,
                         DepenseDepartementRepository depenseRepository,
                         ContratRepository contratRepository) {
        this.departementRepository = departementRepository;
        this.budgetRepository = budgetRepository;
        this.depenseRepository = depenseRepository;
        this.contratRepository = contratRepository;
    }

    @Transactional(readOnly = true)
    public BudgetResponse calculer(Integer annee) {
        int an = annee != null ? annee : LocalDate.now().getYear();
        if (an < 2000 || an > 2100) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Année invalide.");
        }

        Map<Integer, BigDecimal> budgets = new HashMap<>();
        budgetRepository.budgetParDepartement(an).forEach(t -> budgets.put(t.idDepartement(), t.total()));

        Map<Integer, BigDecimal> depenses = new HashMap<>();
        depenseRepository.totalParDepartement(an).forEach(t -> depenses.put(t.idDepartement(), t.total()));

        Map<Integer, BigDecimal> masses = masseSalariale(an);

        List<LigneDepartement> lignes = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalMasse = BigDecimal.ZERO;
        BigDecimal totalDepenses = BigDecimal.ZERO;

        for (Departement d : departementRepository.findAll(Sort.by("nom"))) {
            BigDecimal budget = budgets.getOrDefault(d.getId(), BigDecimal.ZERO);
            BigDecimal masse = masses.getOrDefault(d.getId(), BigDecimal.ZERO);
            BigDecimal depense = depenses.getOrDefault(d.getId(), BigDecimal.ZERO);

            lignes.add(new LigneDepartement(d.getId(), d.getNom(), budget, masse, depense,
                    budget.subtract(masse).subtract(depense)));

            totalBudget = totalBudget.add(budget);
            totalMasse = totalMasse.add(masse);
            totalDepenses = totalDepenses.add(depense);
        }

        Total total = new Total(totalBudget, totalMasse, totalDepenses,
                totalBudget.subtract(totalMasse).subtract(totalDepenses));
        return new BudgetResponse(total, lignes);
    }

    // ----- Masse salariale -----

    /** Salaire mensuel x nombre de mois couverts par le contrat dans l'année, additionné par département. */
    private Map<Integer, BigDecimal> masseSalariale(int annee) {
        LocalDate debutAnnee = LocalDate.of(annee, 1, 1);
        LocalDate finAnnee = LocalDate.of(annee, 12, 31);

        Map<Integer, BigDecimal> masses = new HashMap<>();
        for (LigneSalaire c : contratRepository.lignesSalaire(STATUTS_CONTRAT_COMPTES, debutAnnee, finAnnee)) {
            int mois = moisCouverts(c, debutAnnee, finAnnee);
            if (mois > 0) {
                masses.merge(c.idDepartement(), c.salaire().multiply(BigDecimal.valueOf(mois)), BigDecimal::add);
            }
        }
        return masses;
    }

    /** Nombre de mois (même entamés) pendant lesquels le contrat est en cours dans l'année. */
    private int moisCouverts(LigneSalaire c, LocalDate debutAnnee, LocalDate finAnnee) {
        LocalDate debut = c.dateDebut().isAfter(debutAnnee) ? c.dateDebut() : debutAnnee;
        LocalDate fin = (c.dateFin() == null || c.dateFin().isAfter(finAnnee)) ? finAnnee : c.dateFin();
        if (fin.isBefore(debut)) {
            return 0;
        }
        return fin.getMonthValue() - debut.getMonthValue() + 1;
    }
}
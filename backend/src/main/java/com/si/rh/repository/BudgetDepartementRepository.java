package com.si.rh.repository;

import com.si.rh.dto.TotalDepartement;
import com.si.rh.model.BudgetDepartement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetDepartementRepository extends JpaRepository<BudgetDepartement, Integer> {

    // BY : Fenosoa
    /** Budget alloué à chaque département pour une année. */
    @Query("select new com.si.rh.dto.TotalDepartement(b.departement.id, b.montant) "
            + "from BudgetDepartement b where b.annee = :annee")
    List<TotalDepartement> budgetParDepartement(@Param("annee") Integer annee);
}
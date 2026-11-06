// BY : Brandon
package com.si.rh.repository;

import com.si.rh.model.PlanningTravail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanningTravailRepository extends JpaRepository<PlanningTravail, Integer> {

    long countByRegime_Id(Integer idRegime);

    List<PlanningTravail> findByEmployeIdOrderByDateDebutDesc(Integer idEmploye);

    /**
     * Retourne UN SEUL planning actif pour la date (évite les doublons et les
     * temps prévus multipliés par le nombre de plannings existants).
     */
    @Query(value = """
        SELECT * FROM planning_travail
        WHERE id_employe = :idEmploye
          AND date_debut <= :date
          AND (date_fin IS NULL OR date_fin >= :date)
        ORDER BY date_debut DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PlanningTravail> findByEmployeAndDate(
            @Param("idEmploye") Integer idEmploye,
            @Param("date") LocalDate date);
}
// BY : Brandon
package com.si.rh.repository;

import com.si.rh.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    /** Vérifie si un employé a une mission active à une date donnée (Vérif 6.9). */
    @Query("SELECT COUNT(m) > 0 FROM Mission m " +
           "WHERE m.employe.id = :idEmploye " +
           "AND m.statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE') " +
           "AND :date BETWEEN m.dateDebut AND m.dateFin")
    boolean existsByEmployeAndDate(@Param("idEmploye") Integer idEmploye, @Param("date") LocalDate date);
}
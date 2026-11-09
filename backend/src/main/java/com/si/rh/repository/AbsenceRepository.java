// BY : Brandon
package com.si.rh.repository;

import com.si.rh.model.Absence;
import com.si.rh.model.enums.StatutAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    /** Vérifie si un employé a une absence active à une date donnée (Vérif 6.9). */
    @Query("SELECT COUNT(a) > 0 FROM Absence a " +
           "WHERE a.employe.id = :idEmploye " +
           "AND a.statut <> 'ANNULEE' " +
           "AND :date BETWEEN a.dateDebut AND a.dateFin")
    boolean existsByEmployeAndDate(@Param("idEmploye") Integer idEmploye, @Param("date") LocalDate date);
}
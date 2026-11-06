// BY : Brandon
package com.si.rh.repository;

import com.si.rh.model.TempsTravailJournalier;
import com.si.rh.model.enums.StatutHeuresSup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TempsTravailJournalierRepository extends JpaRepository<TempsTravailJournalier, Integer> {

    Optional<TempsTravailJournalier> findByEmploye_IdAndDateTravail(Integer employeId, LocalDate dateTravail);

    List<TempsTravailJournalier> findByStatutHeuresSupOrderByDateTravailDesc(StatutHeuresSup statut);

    List<TempsTravailJournalier> findByEmploye_IdAndDateTravailBetweenOrderByDateTravailAsc(
            Integer employeId, LocalDate debut, LocalDate fin);
            
    List<TempsTravailJournalier> findByEmploye_Departement_IdAndDateTravailBetweenOrderByEmploye_NomAsc(
            Integer departementId, LocalDate debut, LocalDate fin);
}
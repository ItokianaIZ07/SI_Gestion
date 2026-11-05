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

    @Query("select p from PlanningTravail p where p.employe.id = :idEmploye "
            + "and p.dateDebut <= :date "
            + "and (p.dateFin is null or p.dateFin >= :date)")
    Optional<PlanningTravail> findByEmployeAndDate(
            @Param("idEmploye") Integer idEmploye,
            @Param("date") LocalDate date);
}

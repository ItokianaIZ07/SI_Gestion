package com.si.rh.repository;

import com.si.rh.model.RegleConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RegleCongeRepository extends JpaRepository<RegleConge, Integer> {

    @Query("""
            SELECT r FROM RegleConge r
            WHERE r.typeConge.id = :typeCongeId
              AND r.actif = true
              AND r.dateDebutValidite <= :date
              AND (r.dateFinValidite IS NULL OR r.dateFinValidite >= :date)
            ORDER BY r.dateDebutValidite DESC
            """)
    List<RegleConge> findApplicableRules(
            @Param("typeCongeId") Integer typeCongeId,
            @Param("date") LocalDate date);
}

package com.si.rh.repository;

import com.si.rh.dto.rh.regimeTravail.RegimeAvecUsage;
import com.si.rh.model.RegimeTravail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegimeTravailRepository extends JpaRepository<RegimeTravail, Integer> {

    Optional<RegimeTravail> findByCode(String code);

    List<RegimeTravail> findAllByOrderByLibelleAsc();

    @Query(value = """
            SELECT r.id AS "id",
                r.code AS "code",
                r.libelle AS "libelle",
                r.description AS "description",
                r.heures_hebdomadaires AS "heuresHebdomadaires",
                COUNT(p.id) AS "nbPlannings"
            FROM regime_travail r
            LEFT JOIN planning_travail p ON p.id_regime = r.id
            GROUP BY r.id
            ORDER BY r.libelle
            """, nativeQuery = true)
    List<RegimeAvecUsage> findAllAvecNbPlannings();
}

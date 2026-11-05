package com.si.rh.repository;

import com.si.rh.dto.rh.horaireTravail.HorarireAvecUsage;
import com.si.rh.model.HoraireTravail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoraireTravailRepository extends JpaRepository<HoraireTravail, Integer> {

    Optional<HoraireTravail> findByCode(String code);

    List<HoraireTravail> findAllByOrderByLibelleAsc();

    List<HoraireTravail> findByActifTrueOrderByLibelleAsc();

    @Query(value = """
        SELECT h.*,
            (EXTRACT(EPOCH FROM (h.heure_fin - h.heure_debut)) / 60
                - COALESCE(EXTRACT(EPOCH FROM (h.fin_pause - h.debut_pause)) / 60, 0))::INTEGER AS "minutesPrevues",
            COUNT(p.id) AS "nbPlannings"
        FROM horaire_travail h
        LEFT JOIN planning_travail p ON p.id_horaire = h.id
        GROUP BY h.id
        ORDER BY h.libelle;
        """, nativeQuery = true)
    List<HorarireAvecUsage> findAllAvecNbPlannings();
}

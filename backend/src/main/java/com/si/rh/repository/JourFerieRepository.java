package com.si.rh.repository;

import com.si.rh.dto.rh.jourFerie.JourFerieAvecJourSemaine;
import com.si.rh.model.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JourFerieRepository extends JpaRepository<JourFerie, LocalDate> {

    List<JourFerie> findAllByOrderByDateJourAsc();

    List<JourFerie> findByDateJourBetweenOrderByDateJourAsc(LocalDate debut, LocalDate fin);

    @Query(value = """
            SELECT date_jour AS "dateJour",
                   libelle AS "libelle",
                   EXTRACT(ISODOW FROM date_jour)::INTEGER AS "jourSemaine"
            FROM jour_ferie
            WHERE EXTRACT(YEAR FROM date_jour) = :annee
            ORDER BY date_jour
            """, nativeQuery = true)
    List<JourFerieAvecJourSemaine> findByAnnee(@Param("annee") Integer annee);
}

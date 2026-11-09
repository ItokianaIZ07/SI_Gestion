package com.si.rh.repository;

import com.si.rh.model.HistoriqueAffectation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HistoriqueAffectationRepository extends Repository<HistoriqueAffectation, Integer> {

    @Query("""
            SELECT h FROM HistoriqueAffectation h
            JOIN FETCH h.departement
            JOIN FETCH h.poste
            LEFT JOIN FETCH h.superieur
            WHERE h.employe.id = :idEmploye
            ORDER BY h.dateDebut DESC, h.id DESC
            """)
    List<HistoriqueAffectation> carriere(@Param("idEmploye") Integer idEmploye);

    @Query("""
            SELECT h FROM HistoriqueAffectation h
            LEFT JOIN FETCH h.superieur
            WHERE h.employe.id = :idEmploye AND h.dateFin IS NULL
            """)
    Optional<HistoriqueAffectation> courante(@Param("idEmploye") Integer idEmploye);
}

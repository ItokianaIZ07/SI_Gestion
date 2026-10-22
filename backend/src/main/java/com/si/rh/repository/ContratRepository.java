package com.si.rh.repository;

import com.si.rh.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Integer> {

    @Query("""
        SELECT c FROM Contrat c
        JOIN FETCH c.employe e
        JOIN FETCH c.typeContrat t
        JOIN FETCH c.StatutContract s
        WHERE s.code IN ('BROUILLON', 'EN_ATTENTE_SIGNATURE', 'ACTIF')
        ORDER BY c.id DESC
    """)
    List<Contrat> findActifsEtEnCours();

    @Query("""
        SELECT c FROM Contrat c
        JOIN FETCH c.employe e
        JOIN FETCH c.typeContrat t
        JOIN FETCH c.StatutContract s
        WHERE c.id = :id
    """)
    Optional<Contrat> findByIdAvecDetails(@Param("id") Integer id);
}
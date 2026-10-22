package com.si.rh.repository;

import com.si.rh.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CandidatureRepository extends JpaRepository<Candidature, Integer> {

    @Query("""
        SELECT c FROM Candidature c
        JOIN FETCH c.candidat cd
        JOIN FETCH c.recrutement r
        JOIN FETCH r.besoin b
        JOIN FETCH b.poste p
        JOIN FETCH p.departement d
        WHERE c.statut IN ('RETENU', 'PRESELECTIONNE')
        ORDER BY c.id
    """)
    List<Candidature> findPourEntretien();

    @Query("""
        SELECT c FROM Candidature c
        JOIN FETCH c.candidat cd
        JOIN FETCH c.recrutement r
        JOIN FETCH r.besoin b
        JOIN FETCH b.poste p
        JOIN FETCH p.departement d
        WHERE c.statut = 'RETENU'
          AND NOT EXISTS (SELECT 1 FROM Employe e WHERE e.candidature = c)
        ORDER BY c.id
    """)
    List<Candidature> findRetenusSansEmploye();

    @Query("""
        SELECT c FROM Candidature c
        JOIN FETCH c.candidat
        JOIN FETCH c.recrutement r
        JOIN FETCH r.besoin b
        JOIN FETCH b.poste p
        JOIN FETCH p.departement
        WHERE c.id = :id
    """)
    Optional<Candidature> findByIdAvecDetails(@Param("id") Integer id);
    
    boolean existsByCandidat_IdAndRecrutement_Id(Integer candidatId, Integer recrutementId);
}

package com.si.rh.repository;

import com.si.rh.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Integer> {
    boolean existsByCandidat_IdAndRecrutement_Id(Integer candidatId, Integer recrutementId);
}

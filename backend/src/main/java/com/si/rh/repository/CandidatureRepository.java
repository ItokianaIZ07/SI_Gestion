package com.sigrh.repository;

import com.sigrh.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Integer> {
}

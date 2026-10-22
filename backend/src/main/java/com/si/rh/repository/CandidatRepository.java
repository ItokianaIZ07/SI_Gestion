package com.si.rh.repository;

import com.si.rh.model.Candidat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CandidatRepository extends JpaRepository<Candidat, Integer> {
    Optional<Candidat> findByEmail(String email);
}

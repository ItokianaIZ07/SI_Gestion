package com.si.rh.repository;

import com.si.rh.model.Entretien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntretienRepository extends JpaRepository<Entretien, Integer> {
    List<Entretien> findByCandidatureIdOrderByDateEntretienDesc(Integer idCandidature);
}
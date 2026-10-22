package com.si.rh.repository;

import com.si.rh.model.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Integer> {
    boolean existsByRecrutement_Id(Integer recrutementId);
}

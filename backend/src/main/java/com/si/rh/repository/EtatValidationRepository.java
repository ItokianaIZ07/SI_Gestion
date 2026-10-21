package com.si.rh.repository;

import com.si.rh.model.EtatValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EtatValidationRepository extends JpaRepository<EtatValidation, Integer> {
    Optional<EtatValidation> findByCode(String code);
}

package com.sigrh.repository;

import com.sigrh.model.EtatValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EtatValidationRepository extends JpaRepository<EtatValidation, Integer> {
    Optional<EtatValidation> findByCode(String code);
}

package com.si.rh.repository;

import com.si.rh.model.ValidationConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @Author Itokiana
 * */

public interface ValidationCongeRepository extends JpaRepository<ValidationConge, Integer> {
    Optional<ValidationConge> findByDemandeConge_IdAndOrdre(
            Integer demandeId,
            int ordre);
}

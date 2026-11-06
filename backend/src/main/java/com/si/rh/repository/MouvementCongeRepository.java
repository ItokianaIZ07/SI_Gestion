package com.si.rh.repository;

import com.si.rh.model.MouvementConge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MouvementCongeRepository extends JpaRepository<MouvementConge, Long> {
    boolean existsByDemandeConge_IdAndTypeMouvement(
            Integer demandeCongeId,
            String typeMouvement);
}

package com.si.rh.repository;

import com.si.rh.model.MouvementConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MouvementCongeRepository extends JpaRepository<MouvementConge, Long> {
    List<MouvementConge> findAllByDemandeConge_IdAndTypeMouvement(
            Integer demandeCongeId,
            String typeMouvement);

    boolean existsByDemandeConge_IdAndTypeMouvement(
            Integer demandeCongeId,
            String typeMouvement);
}

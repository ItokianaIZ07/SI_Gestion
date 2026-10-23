package com.si.rh.repository;

import com.si.rh.model.TypeContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface TypeContratRepository extends JpaRepository<TypeContrat, Integer> {
    Optional<TypeContrat> findByCode(String code);
    List<TypeContrat> findAllByOrderByLibelleAsc();
}

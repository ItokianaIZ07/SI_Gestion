package com.sigrh.repository;

import com.sigrh.model.TypeContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TypeContratRepository extends JpaRepository<TypeContrat, Integer> {
    Optional<TypeContrat> findByCode(String code);
}

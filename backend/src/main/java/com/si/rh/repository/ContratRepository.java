package com.si.rh.repository;

import com.si.rh.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Integer> {
    Optional<Contrat> findByReference(String reference);
}

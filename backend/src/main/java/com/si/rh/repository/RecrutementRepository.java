package com.si.rh.repository;

import com.si.rh.model.Recrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RecrutementRepository extends JpaRepository<Recrutement, Integer> {
    Optional<Recrutement> findByReference(String reference);
}

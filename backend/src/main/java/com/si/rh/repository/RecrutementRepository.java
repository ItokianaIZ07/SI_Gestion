package com.sigrh.repository;

import com.sigrh.model.Recrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RecrutementRepository extends JpaRepository<Recrutement, Integer> {
    Optional<Recrutement> findByReference(String reference);
}

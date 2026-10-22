package com.si.rh.repository;

import com.si.rh.model.HistoriqueContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HistoriqueContratRepository extends JpaRepository<HistoriqueContrat, Integer> {
    long countByEvenementContainingAndContratId(String evenement, Integer contratId);

}

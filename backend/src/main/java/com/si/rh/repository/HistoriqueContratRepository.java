package com.sigrh.repository;

import com.sigrh.model.HistoriqueContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HistoriqueContratRepository extends JpaRepository<HistoriqueContrat, Integer> {
}

package com.sigrh.repository;

import com.sigrh.model.DocumentEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentEmployeRepository extends JpaRepository<DocumentEmploye, Integer> {
}

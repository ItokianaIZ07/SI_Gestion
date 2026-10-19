package com.sigrh.repository;

import com.sigrh.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Integer> {
    Optional<Employe> findByMatricule(String matricule);
}

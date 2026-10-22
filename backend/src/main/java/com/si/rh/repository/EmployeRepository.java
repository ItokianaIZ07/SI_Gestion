//Brandon
package com.si.rh.repository;


import com.si.rh.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;



public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByMatricule(String matricule);
    
    @Query("SELECT e FROM Employe e WHERE e.candidature.id = :idCandidature")
    Optional<Employe> findByIdCandidature(@Param("idCandidature") Integer idCandidature);

    @Query("SELECT COUNT(e) > 0 FROM Employe e WHERE e.candidature.id = :idCandidature")
    boolean existsByIdCandidature(@Param("idCandidature") Integer idCandidature);
}
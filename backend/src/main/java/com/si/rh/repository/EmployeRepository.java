//Brandon
package com.si.rh.repository;

import com.si.rh.model.Employe;
import com.si.rh.model.enums.StatutEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.List;

public interface EmployeRepository extends JpaRepository<Employe, Integer> {

    Optional<Employe> findByMatricule(String matricule);

    List<Employe> findByStatutOrderByNomAscPrenomAsc(StatutEmploye statut);

    @Query("SELECT e FROM Employe e WHERE e.candidature.id = :idCandidature")
    Optional<Employe> findByIdCandidature(@Param("idCandidature") Integer idCandidature);

    @Query("SELECT COUNT(e) > 0 FROM Employe e WHERE e.candidature.id = :idCandidature")
    boolean existsByIdCandidature(@Param("idCandidature") Integer idCandidature);
}

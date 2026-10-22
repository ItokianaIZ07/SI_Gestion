package com.si.rh.repository;

import com.si.rh.model.Employe;
import com.si.rh.model.enums.StatutEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Integer> {
    Optional<Employe> findByMatricule(String matricule);
    List<Employe> findByStatutOrderByNomAscPrenomAsc(StatutEmploye statut);
}

package com.si.rh.repository;

import com.si.rh.model.Besoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BesoinRepository extends JpaRepository<Besoin, Integer> {
    List<Besoin> findByPoste_Departement_IdOrderByDateDemandeDesc(Integer departementId);
    long countByPoste_Departement_Id(Integer departementId);
}

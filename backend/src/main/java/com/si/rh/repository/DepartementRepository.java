package com.si.rh.repository;

import com.si.rh.model.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Integer> {
    List<Departement> findAllByOrderByNomAsc();
}

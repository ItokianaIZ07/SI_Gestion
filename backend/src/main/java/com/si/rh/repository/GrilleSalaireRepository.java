package com.si.rh.repository;

import com.si.rh.model.GrilleSalaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GrilleSalaireRepository extends JpaRepository<GrilleSalaire, Integer> {
}

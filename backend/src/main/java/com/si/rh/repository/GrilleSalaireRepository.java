package com.sigrh.repository;

import com.sigrh.model.GrilleSalaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GrilleSalaireRepository extends JpaRepository<GrilleSalaire, Integer> {
}

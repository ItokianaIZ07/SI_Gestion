package com.sigrh.repository;

import com.sigrh.model.DepenseDepartement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DepenseDepartementRepository extends JpaRepository<DepenseDepartement, Integer> {
}

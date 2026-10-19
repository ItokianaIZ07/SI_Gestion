package com.sigrh.repository;

import com.sigrh.model.ValidationBesoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ValidationBesoinRepository extends JpaRepository<ValidationBesoin, Integer> {
}

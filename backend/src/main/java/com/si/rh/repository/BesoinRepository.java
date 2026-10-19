package com.sigrh.repository;

import com.sigrh.model.Besoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BesoinRepository extends JpaRepository<Besoin, Integer> {
}

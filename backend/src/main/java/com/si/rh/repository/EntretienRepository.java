package com.sigrh.repository;

import com.sigrh.model.Entretien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EntretienRepository extends JpaRepository<Entretien, Integer> {
}

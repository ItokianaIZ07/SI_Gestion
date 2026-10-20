package com.si.rh.repository;

import com.si.rh.model.Entretien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EntretienRepository extends JpaRepository<Entretien, Integer> {
}

package com.sigrh.repository;

import com.sigrh.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Integer> {
    Optional<Decision> findByCode(String code);
}

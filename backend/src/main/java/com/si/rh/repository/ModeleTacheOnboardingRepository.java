package com.sigrh.repository;

import com.sigrh.model.ModeleTacheOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ModeleTacheOnboardingRepository extends JpaRepository<ModeleTacheOnboarding, Integer> {
    Optional<ModeleTacheOnboarding> findByCode(String code);
}

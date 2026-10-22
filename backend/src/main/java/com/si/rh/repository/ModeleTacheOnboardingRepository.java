package com.si.rh.repository;

import com.si.rh.model.ModeleTacheOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;


@Repository
public interface ModeleTacheOnboardingRepository extends JpaRepository<ModeleTacheOnboarding, Integer> {
    Optional<ModeleTacheOnboarding> findByCode(String code);

    List<ModeleTacheOnboarding> findByActifTrue();
}

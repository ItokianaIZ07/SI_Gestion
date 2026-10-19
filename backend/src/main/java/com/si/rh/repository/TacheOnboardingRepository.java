package com.sigrh.repository;

import com.sigrh.model.TacheOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TacheOnboardingRepository extends JpaRepository<TacheOnboarding, Integer> {
}

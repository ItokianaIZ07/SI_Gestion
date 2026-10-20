package com.si.rh.repository;

import com.si.rh.model.TacheOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TacheOnboardingRepository extends JpaRepository<TacheOnboarding, Integer> {
}

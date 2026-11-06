package com.si.rh.repository;

import com.si.rh.model.HoraireTravail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoraireTravailRepository extends JpaRepository<HoraireTravail, Integer> {

    Optional<HoraireTravail> findByCode(String code);

    List<HoraireTravail> findAllByOrderByLibelleAsc();

    List<HoraireTravail> findByActifTrueOrderByLibelleAsc();
}

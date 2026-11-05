package com.si.rh.repository;

import com.si.rh.model.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JourFerieRepository extends JpaRepository<JourFerie, LocalDate> {

    List<JourFerie> findAllByOrderByDateJourAsc();

    List<JourFerie> findByDateJourBetweenOrderByDateJourAsc(LocalDate debut, LocalDate fin);
}

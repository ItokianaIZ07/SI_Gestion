package com.si.rh.repository;

import com.si.rh.model.TypeConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TypeCongeRepository extends JpaRepository<TypeConge, Integer> {
    List<TypeConge> findByActifTrueOrderByLibelleAsc();
}

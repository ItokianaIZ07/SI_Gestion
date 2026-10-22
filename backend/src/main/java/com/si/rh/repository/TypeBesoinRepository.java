package com.si.rh.repository;

import com.si.rh.model.TypeBesoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface TypeBesoinRepository extends JpaRepository<TypeBesoin, Integer> {
    Optional<TypeBesoin> findByCode(String code);
    List<TypeBesoin> findAllByOrderByLibelleAsc();
}

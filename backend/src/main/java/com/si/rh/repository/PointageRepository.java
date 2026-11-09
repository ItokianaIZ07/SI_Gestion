// BY : Brandon
package com.si.rh.repository;

import com.si.rh.model.Pointage;
import com.si.rh.model.enums.TypePointage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PointageRepository extends JpaRepository<Pointage, Integer> {

    List<Pointage> findByEmploye_IdAndDateHeureBetweenOrderByDateHeureAsc(
            Integer employeId, OffsetDateTime debut, OffsetDateTime fin);

    boolean existsByEmploye_IdAndDateHeureAndTypePointage(
            Integer employeId, OffsetDateTime dateHeure, TypePointage typePointage);

    List<Pointage> findByEmploye_IdOrderByDateHeureDesc(Integer employeId);
}
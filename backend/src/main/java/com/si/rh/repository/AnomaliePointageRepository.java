// BY : Brandon
package com.si.rh.repository;

import com.si.rh.model.AnomaliePointage;
import com.si.rh.model.enums.StatutAnomalie;
import com.si.rh.model.enums.TypeAnomalie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnomaliePointageRepository extends JpaRepository<AnomaliePointage, Integer> {

    List<AnomaliePointage> findByEmploye_IdAndDateAnomalieBetweenOrderByDateAnomalieDesc(
            Integer employeId, LocalDate debut, LocalDate fin);

    List<AnomaliePointage> findByStatutOrderByDateAnomalieDesc(StatutAnomalie statut);

    boolean existsByEmploye_IdAndDateAnomalieAndTypeAnomalie(
            Integer employeId, LocalDate dateAnomalie, TypeAnomalie typeAnomalie);
}
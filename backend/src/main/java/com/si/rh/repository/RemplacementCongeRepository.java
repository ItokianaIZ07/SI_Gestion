package com.si.rh.repository;

import com.si.rh.model.RemplacementConge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RemplacementCongeRepository extends JpaRepository<RemplacementConge, Long> {
    List<RemplacementConge> findAllByDemandeConge_IdIn(List<Integer> demandeIds);

    @EntityGraph(attributePaths = {
            "demandeConge",
            "demandeConge.employe",
            "demandeConge.typeConge"
    })
    List<RemplacementConge> findAllByEmployeRemplacant_IdOrderByDatePropositionDesc(
            Integer employeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM RemplacementConge r
            JOIN FETCH r.demandeConge d
            JOIN FETCH d.employe
            JOIN FETCH d.typeConge
            JOIN FETCH r.employeRemplacant
            WHERE r.id = :id
            """)
    Optional<RemplacementConge> findByIdForUpdate(@Param("id") Long id);
}

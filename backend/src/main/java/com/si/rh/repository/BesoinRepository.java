package com.si.rh.repository;

import com.si.rh.model.Besoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface BesoinRepository extends JpaRepository<Besoin, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Besoin b where b.id = :id")
    Optional<Besoin> findByIdForUpdate(@Param("id") Integer id);
}

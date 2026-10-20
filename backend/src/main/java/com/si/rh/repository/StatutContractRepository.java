package com.si.rh.repository;

import com.si.rh.model.StatutContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StatutContractRepository extends JpaRepository<StatutContract, Integer> {
    Optional<StatutContract> findByCode(String code);
}

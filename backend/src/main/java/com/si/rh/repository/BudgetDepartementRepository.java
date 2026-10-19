package com.sigrh.repository;

import com.sigrh.model.BudgetDepartement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BudgetDepartementRepository extends JpaRepository<BudgetDepartement, Integer> {
}

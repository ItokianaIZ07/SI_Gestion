package com.si.rh.repository;

import com.si.rh.model.ValidationBesoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ValidationBesoinRepository extends JpaRepository<ValidationBesoin, Integer> {
    @Query("""
            select vb from ValidationBesoin vb
            join fetch vb.etatValidation
            join fetch vb.decision
            where vb.besoin.id in :besoinIds
            """)
    List<ValidationBesoin> findAllForBesoins(@Param("besoinIds") Collection<Integer> besoinIds);
}

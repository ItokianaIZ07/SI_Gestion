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

    // BY : Fenosoa
    @Query("select v from ValidationBesoin v join fetch v.etatValidation join fetch v.decision "
            + "where v.besoin.id in :ids")
    List<ValidationBesoin> findByBesoinIds(@Param("ids") Collection<Integer> ids);
}
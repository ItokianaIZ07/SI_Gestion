package com.si.rh.repository;

import com.si.rh.dto.TotalDepartement;
import com.si.rh.model.DepenseDepartement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepenseDepartementRepository extends JpaRepository<DepenseDepartement, Integer> {

    // BY : Fenosoa
    /** Total des dépenses internes de chaque département pour une année. */
    @Query("select new com.si.rh.dto.TotalDepartement(d.departement.id, sum(d.montant)) "
            + "from DepenseDepartement d where d.annee = :annee group by d.departement.id")
    List<TotalDepartement> totalParDepartement(@Param("annee") Integer annee);
}
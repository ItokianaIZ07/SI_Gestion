package com.si.rh.repository;

import com.si.rh.model.DemandeConge;
import com.si.rh.projection.VCongeDemandesProjection;
import com.si.rh.projection.VSoldeCongeEmployeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Integer> {

    @Query(
        value = """
                    SELECT
                        v.id_employe as idEmploye,
                        v.id_type_conge as idTypeConge,
                        v.code_type_conge as codeTypeConge,
                        v.type_conge as typeConge,
                        v.solde_disponible as soldeDisponible
                    FROM v_conge_solde_employe v
                """,
            nativeQuery = true
    )
    List<VSoldeCongeEmployeProjection> getSoldeConge();

    @Query(value = "SELECT * FROM v_conge_demandes WHERE id_employe = :idEmploye", nativeQuery = true)
    List<VCongeDemandesProjection> findAllDemandesByEmployeId(@Param("idEmploye") Integer idEmploye);

    @Query(value = "SELECT * FROM v_conge_demandes", nativeQuery = true)
    List<VCongeDemandesProjection> getAllDemandesVue();
}

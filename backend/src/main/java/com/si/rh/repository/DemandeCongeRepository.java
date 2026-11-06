package com.si.rh.repository;

import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.model.DemandeConge;
import com.si.rh.projection.VCongeDemandesProjection;
import com.si.rh.projection.VSoldeCongeEmployeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 *
 * @Author Itokiana
 * */

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

    @Query(value = """
        SELECT
            e.id AS idEmploye,
            e.nom AS nomEmploye,
            e.prenom AS prenomEmploye,
            e.matricule AS matriculeEmploye,
            e.email AS emailEmploye,
            tc.libelle AS typeConge,
            COALESCE(SUM(mc.nombre_jours), 0)::DOUBLE PRECISION AS solde
        FROM employe e
        CROSS JOIN type_conge tc
        LEFT JOIN mouvement_conge mc
            ON mc.id_employe = e.id
            AND mc.id_type_conge = tc.id
        WHERE e.id = :idEmploye
          AND tc.id = :idTypeConge
        GROUP BY
            e.id,
            e.nom,
            e.prenom,
            e.matricule,
            e.email,
            tc.libelle
        """, nativeQuery = true)
    Optional<SoldeCongeEmployeDTO> getSoldeByEmpAndTypeConge(
            @Param("idEmploye") Integer idEmploye,
            @Param("idTypeConge") Integer idTypeConge
    );
}

package com.si.rh.repository;

import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.model.DemandeConge;
import com.si.rh.projection.VCongeDemandesProjection;
import com.si.rh.projection.VCongeValidationProjection;
import com.si.rh.projection.VSoldeCongeEmployeProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
        SELECT v.*
        FROM v_conge_demandes_en_attente v
        JOIN employe e ON e.id = v.id_employe
        JOIN departement d ON d.id = e.id_departement
        WHERE v.statut_global = 'EN_ATTENTE_MANAGER'
          AND d.id_utilisateur_manager = :idManager
        """, nativeQuery = true)
    List<VCongeDemandesProjection> findDemandesEnAttenteManager(
            @Param("idManager") Integer idManager);

    @Query(value = """
        SELECT *
        FROM v_conge_demandes_en_attente
        WHERE statut_global = 'EN_ATTENTE_RH'
        """, nativeQuery = true)
    List<VCongeDemandesProjection> findDemandesEnAttenteRh();

    @Query(value = """
        SELECT
            v.id_validation AS idValidation,
            v.id_demande_conge AS idDemandeConge,
            v.id_utilisateur AS idUtilisateur,
            u.email AS emailValidateur,
            v.ordre AS ordre,
            v.etape_validation AS etapeValidation,
            v.code_decision AS codeDecision,
            v.commentaire AS commentaire,
            v.date_validation AS dateValidation
        FROM v_conge_validations v
        LEFT JOIN utilisateur u ON u.id = v.id_utilisateur
        WHERE v.id_demande_conge = :idDemande
        ORDER BY v.ordre
        """, nativeQuery = true)
    List<VCongeValidationProjection> findValidationsByDemandeId(
            @Param("idDemande") Integer idDemande);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DemandeConge d WHERE d.id = :id")
    Optional<DemandeConge> findByIdForUpdate(@Param("id") Integer id);

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

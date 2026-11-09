//Brandon
package com.si.rh.repository;

import com.si.rh.model.Employe;
import com.si.rh.model.enums.StatutEmploye;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe, Integer> {

    Optional<Employe> findByMatricule(String matricule);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employe e WHERE e.id = :id")
    Optional<Employe> findByIdForUpdate(@Param("id") Integer id);

    Optional<Employe> findByMatriculeAndEmailAndNumeroCinAndStatut(
            String matricule,
            String email,
            String numeroCin,
            StatutEmploye statut);

    List<Employe> findByStatutOrderByNomAscPrenomAsc(StatutEmploye statut);

    List<Employe> findByDepartement_IdAndStatutOrderByNomAscPrenomAsc(
            Integer departementId,
            StatutEmploye statut);
    List<Employe> findByStatutInOrderByNomAscPrenomAsc(List<StatutEmploye> statuts);

    @Query("SELECT e FROM Employe e WHERE e.candidature.id = :idCandidature")
    Optional<Employe> findByIdCandidature(@Param("idCandidature") Integer idCandidature);

    @Query("SELECT COUNT(e) > 0 FROM Employe e WHERE e.candidature.id = :idCandidature")
    boolean existsByIdCandidature(@Param("idCandidature") Integer idCandidature);

    List<Employe> findByDepartement_Id(Integer departementId);

    long countByDepartement_IdAndStatut(Integer departementId, StatutEmploye statut);

    @Query("SELECT e FROM Employe e JOIN FETCH e.poste JOIN FETCH e.departement ORDER BY e.nom, e.prenom")
    List<Employe> findAllAvecPosteEtDepartement();

    @Query("""
            SELECT e FROM Employe e
            JOIN FETCH e.poste JOIN FETCH e.departement
            LEFT JOIN FETCH e.utilisateur
            WHERE e.id = :id
            """)
    Optional<Employe> findByIdAvecDetails(@Param("id") Integer id);

    // BY : Brandon
    /** Vérifie si un employé est subordonné (direct ou indirect) d'un manager. */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM lister_subordonnes(:idManager, false) s
            WHERE s.sub_id = :idEmploye
        )
        """, nativeQuery = true)
    boolean estSubordonneDe(@Param("idEmploye") Integer idEmploye, @Param("idManager") Integer idManager);
}

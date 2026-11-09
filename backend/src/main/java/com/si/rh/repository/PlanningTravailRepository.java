// BY : Brandon
package com.si.rh.repository;

import com.si.rh.dto.rh.planningTravail.PlanningTravailAvecDetails;
import com.si.rh.model.HoraireTravail;
import com.si.rh.model.PlanningTravail;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanningTravailRepository extends JpaRepository<PlanningTravail, Integer> {

    long countByRegime_Id(Integer idRegime);

    long countByHoraire_Id(Integer idHoraire);

    List<PlanningTravail> findByEmployeIdOrderByDateDebutDesc(Integer idEmploye);

    Optional<PlanningTravail> findFirstByEmploye_IdAndDateFinIsNullAndDateDebutBeforeOrderByDateDebutDesc(
            Integer idEmploye, LocalDate dateDebut);

    @Query(value = """
        SELECT *
        FROM planning_travail p
        WHERE p.id_employe = :idEmploye
          AND (CAST(:idIgnore AS INTEGER) IS NULL OR p.id <> :idIgnore)
          AND p.date_debut <= COALESCE(CAST(:dateFin AS DATE), DATE 'infinity')
          AND COALESCE(p.date_fin, DATE 'infinity') >= :dateDebut
          AND (NOT :autoriserCloture OR p.date_fin IS NOT NULL OR p.date_debut >= :dateDebut)
        ORDER BY p.date_debut
        LIMIT 1
        """, nativeQuery = true)
    Optional<PlanningTravail> findChevauchement(
            @Param("idEmploye") Integer idEmploye,
            @Param("idIgnore") Integer idIgnore,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("autoriserCloture") boolean autoriserCloture);

    @Query(value = """
        SELECT p.id AS "id",
               e.id AS "idEmploye",
               d.id AS "idDepartement",
               e.matricule AS "matricule",
               e.nom AS "nom",
               e.prenom AS "prenom",
               d.nom AS "departement",
               h.libelle AS "horaire",
               r.libelle AS "regime",
               p.date_debut AS "dateDebut",
               p.date_fin AS "dateFin",
               (p.date_debut <= CURRENT_DATE
                AND COALESCE(p.date_fin, DATE 'infinity') >= CURRENT_DATE) AS "enCours"
        FROM planning_travail p
        JOIN employe e ON e.id = p.id_employe
        JOIN departement d ON d.id = e.id_departement
        JOIN horaire_travail h ON h.id = p.id_horaire
        LEFT JOIN regime_travail r ON r.id = p.id_regime
        WHERE (CAST(:idEmploye AS INTEGER) IS NULL OR p.id_employe = :idEmploye)
          AND (CAST(:idDepartement AS INTEGER) IS NULL OR e.id_departement = :idDepartement)
          AND (CAST(:idHoraire AS INTEGER) IS NULL OR p.id_horaire = :idHoraire)
          AND (CAST(:dateDebut AS DATE) IS NULL OR COALESCE(p.date_fin, DATE 'infinity') >= :dateDebut)
          AND (CAST(:dateFin AS DATE) IS NULL OR p.date_debut <= :dateFin)
        ORDER BY e.nom, e.prenom, p.date_debut DESC
        """, nativeQuery = true)
    List<PlanningTravailAvecDetails> findAllAvecFiltres(
            @Param("idEmploye") Integer idEmploye,
            @Param("idDepartement") Integer idDepartement,
            @Param("idHoraire") Integer idHoraire,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    @Query(value = """
        SELECT p.id AS "id",
               e.id AS "idEmploye",
               d.id AS "idDepartement",
               e.matricule AS "matricule",
               e.nom AS "nom",
               e.prenom AS "prenom",
               d.nom AS "departement",
               h.libelle AS "horaire",
               r.libelle AS "regime",
               p.date_debut AS "dateDebut",
               p.date_fin AS "dateFin",
               (p.date_debut <= CURRENT_DATE
                AND COALESCE(p.date_fin, DATE 'infinity') >= CURRENT_DATE) AS "enCours"
        FROM planning_travail p
        JOIN employe e ON e.id = p.id_employe
        JOIN departement d ON d.id = e.id_departement
        JOIN horaire_travail h ON h.id = p.id_horaire
        LEFT JOIN regime_travail r ON r.id = p.id_regime
        WHERE utilisateur_est_manager_de(:idUtilisateur, e.id)
          AND (CAST(:idEmploye AS INTEGER) IS NULL OR p.id_employe = :idEmploye)
          AND (CAST(:idDepartement AS INTEGER) IS NULL OR e.id_departement = :idDepartement)
          AND (CAST(:idHoraire AS INTEGER) IS NULL OR p.id_horaire = :idHoraire)
          AND (CAST(:dateDebut AS DATE) IS NULL OR COALESCE(p.date_fin, DATE 'infinity') >= :dateDebut)
          AND (CAST(:dateFin AS DATE) IS NULL OR p.date_debut <= :dateFin)
        ORDER BY e.nom, e.prenom, p.date_debut DESC
        """, nativeQuery = true)
    List<PlanningTravailAvecDetails> findAllAvecFiltresManager(
            @Param("idUtilisateur") Integer idUtilisateur,
            @Param("idEmploye") Integer idEmploye,
            @Param("idDepartement") Integer idDepartement,
            @Param("idHoraire") Integer idHoraire,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    @Query(value = """
        SELECT utilisateur_est_manager_de(:idUtilisateur, :idEmploye)
        """, nativeQuery = true)
    boolean estEmployeDansEquipeManager(
            @Param("idUtilisateur") Integer idUtilisateur,
            @Param("idEmploye") Integer idEmploye);

    @Query("""
        select h from PlanningTravail p
        join p.horaire h
        where p.employe.id = :idEmploye
          and p.dateDebut <= :date
          and (p.dateFin is null or p.dateFin >= :date)
        """)
    Optional<HoraireTravail> findHoraireEnVigueur(
            @Param("idEmploye") Integer idEmploye,
            @Param("date") LocalDate date);

    @Modifying
    @Query("""
        update PlanningTravail p
        set p.dateFin = :dateFin
        where p.employe.id = :idEmploye
          and p.dateFin is null
          and p.dateDebut < :nouveauDebut
        """)
    int cloturerPlanningsOuvertsAnterieurs(
            @Param("idEmploye") Integer idEmploye,
            @Param("nouveauDebut") LocalDate nouveauDebut,
            @Param("dateFin") LocalDate dateFin);

    @Query("""
        select count(t) from TempsTravailJournalier t
        where t.employe.id = :idEmploye
          and t.dateTravail >= :dateDebut
          and (:dateFin is null or t.dateTravail <= :dateFin)
        """)
    long countJourneesCalculees(
            @Param("idEmploye") Integer idEmploye,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    /**
     * Retourne UN SEUL planning actif pour la date (évite les doublons et les
     * temps prévus multipliés par le nombre de plannings existants).
     */
    @Query(value = """
        SELECT * FROM planning_travail
        WHERE id_employe = :idEmploye
          AND date_debut <= :date
          AND (date_fin IS NULL OR date_fin >= :date)
        ORDER BY date_debut DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PlanningTravail> findByEmployeAndDate(
            @Param("idEmploye") Integer idEmploye,
            @Param("date") LocalDate date);
}
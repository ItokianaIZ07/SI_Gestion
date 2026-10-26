package com.si.rh.repository;

import com.si.rh.model.GrilleSalaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GrilleSalaireRepository extends JpaRepository<GrilleSalaire, Integer> {

    // BY : Fenosoa
    /** Toutes les grilles avec leur poste et leur département (évite les requêtes en cascade). */
    @Query("select g from GrilleSalaire g join fetch g.poste p join fetch p.departement d "
            + "order by d.nom, p.nom, g.dateDebutValidite desc")
    List<GrilleSalaire> findAllAvecPoste();

    /** Toutes les grilles d'un poste (pour vérifier le chevauchement des périodes). */
    List<GrilleSalaire> findByPosteId(Integer idPoste);

    List<GrilleSalaire> findByPosteDepartementId(Integer idDepartement);
    /** Grille d'un poste valable à une date donnée (les périodes ne se chevauchent pas : au plus une). */
    @Query("select g from GrilleSalaire g where g.poste.id = :idPoste "
            + "and g.dateDebutValidite <= :date "
            + "and (g.dateFinValidite is null or g.dateFinValidite >= :date) "
            + "order by g.dateDebutValidite desc")
    List<GrilleSalaire> findEnVigueur(@Param("idPoste") Integer idPoste, @Param("date") LocalDate date);
}

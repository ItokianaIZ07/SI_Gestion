package com.si.rh.repository;

import com.si.rh.dto.LigneSalaire;
import com.si.rh.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Integer> {
    Optional<Contrat> findByReference(String reference);

    // BY : Fenosoa
    /**
     * Salaires des contrats qui chevauchent l'année demandée (le PDF du contrat n'est pas chargé).
     * Seuls les contrats dont le statut est dans {@code codesStatut} sont retenus.
     */
    @Query("select new com.si.rh.dto.LigneSalaire(e.departement.id, c.salaire, c.dateDebut, c.dateFin) "
            + "from Contrat c join c.employe e "
            + "where c.statutContrat.code in :codesStatut and c.salaire is not null "
            + "and c.dateDebut <= :finAnnee and (c.dateFin is null or c.dateFin >= :debutAnnee)")
    List<LigneSalaire> lignesSalaire(@Param("codesStatut") Collection<String> codesStatut,
                                     @Param("debutAnnee") LocalDate debutAnnee,
                                     @Param("finAnnee") LocalDate finAnnee);
}
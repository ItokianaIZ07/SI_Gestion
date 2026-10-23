package com.si.rh.repository;

import com.si.rh.model.Besoin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BesoinRepository extends JpaRepository<Besoin, Integer> {

    // BY : Fenosoa
    /** Demandes affichées dans le module Finance (les demandes annulées sont masquées). */
    @Query("select b from Besoin b join fetch b.poste p join fetch p.departement "
            + "where b.statutGlobal <> com.si.rh.model.enums.StatutGlobalBesoin.ANNULE "
            + "order by b.dateDemande desc")
    List<Besoin> findPourFinance();

    /** Verrouille la demande pendant la validation (deux clics simultanés ne se mélangent pas). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Besoin b where b.id = :id")
    Optional<Besoin> findByIdPourMiseAJour(@Param("id") Integer id);
  
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Besoin b where b.id = :id")
    Optional<Besoin> findByIdForUpdate(@Param("id") Integer id);

    List<Besoin> findByPoste_Departement_IdOrderByDateDemandeDesc(Integer departementId);
    long countByPoste_Departement_Id(Integer departementId);
}

package com.si.rh.repository;

import com.si.rh.model.Poste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PosteRepository extends JpaRepository<Poste, Integer> {
    // BY : Fenosoa
    /** Postes actifs avec leur département, triés par département puis par nom. */
    @Query("select p from Poste p join fetch p.departement d where p.actif = true order by d.nom, p.nom")
    List<Poste> findActifsAvecDepartement();
}

package com.si.rh.repository;

import com.si.rh.dto.document.DocumentAdminLigne;
import com.si.rh.dto.document.DocumentAlerteLigne;
import com.si.rh.dto.onboarding.DocumentEmployeDTO;
import com.si.rh.model.DocumentEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentEmployeRepository extends JpaRepository<DocumentEmploye, Integer> {

    /** Métadonnées uniquement : la colonne BYTEA n'est pas chargée pour afficher une liste. */
    @Query("""
            SELECT new com.si.rh.dto.onboarding.DocumentEmployeDTO(
                d.id, d.typeDocument, d.nomFichier, d.mimeType, d.statut, d.dateDepot)
            FROM DocumentEmploye d
            WHERE d.employe.id = :idEmploye
            ORDER BY d.dateDepot DESC, d.id DESC
            """)
    List<DocumentEmployeDTO> findResumesParEmploye(@Param("idEmploye") Integer idEmploye);

    /** Documents d'un employé avec leur validité (la colonne BYTEA n'est pas chargée). */
    @Query("""
            SELECT new com.si.rh.dto.document.DocumentAdminLigne(
                d.id, d.typeDocument, d.nomFichier, d.mimeType, d.statut, d.dateDepot,
                d.dateExpiration, d.dateRenouvellement, d.delaiAlerteJours)
            FROM DocumentEmploye d
            WHERE d.employe.id = :idEmploye
            ORDER BY d.dateDepot DESC, d.id DESC
            """)
    List<DocumentAdminLigne> findLignesAdminParEmploye(@Param("idEmploye") Integer idEmploye);

    /** Documents qui expirent un jour, non renouvelés, non refusés, d'employés encore en poste. */
    @Query("""
            SELECT new com.si.rh.dto.document.DocumentAlerteLigne(
                d.id, e.id, e.matricule, e.nom, e.prenom, d.typeDocument, d.statut,
                d.dateExpiration, d.delaiAlerteJours)
            FROM DocumentEmploye d JOIN d.employe e
            WHERE d.dateExpiration IS NOT NULL AND d.dateRenouvellement IS NULL
              AND d.statut <> com.si.rh.model.enums.StatutDocument.REFUSE
              AND e.statut <> com.si.rh.model.enums.StatutEmploye.PARTI
            ORDER BY d.dateExpiration ASC, d.id ASC
            """)
    List<DocumentAlerteLigne> findCandidatsAlerte();
}

package com.si.rh.repository;

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
}

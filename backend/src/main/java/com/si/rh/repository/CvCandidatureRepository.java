package com.si.rh.repository;

import com.si.rh.dto.rh.CandidatureRhDTO;
import com.si.rh.model.Cv;
import com.si.rh.model.enums.StatutLecture;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Lectures « liste des CV / candidatures » pour les RH.
 * Les requêtes sélectionnent des colonnes précises : la colonne BYTEA du CV n'est jamais chargée.
 */
public interface CvCandidatureRepository extends Repository<Cv, Integer> {

    /** Tous les CV déposés, avec leur candidature éventuelle (dépôts spontanés inclus). */
    @Query("""
            SELECT new com.si.rh.dto.rh.CandidatureRhDTO(
                cv.id, cd.id, cd.nom, cd.prenom, cd.email, cd.telephone, cd.experience,
                cv.nomFichier, cv.dateDepot, cv.statutLecture,
                c.id, c.statut, c.dateCandidature,
                r.id, r.reference, r.titre, p.nom)
            FROM Cv cv
            JOIN cv.candidat cd
            LEFT JOIN Candidature c ON c.cv = cv
            LEFT JOIN c.recrutement r
            LEFT JOIN r.besoin b
            LEFT JOIN b.poste p
            ORDER BY cv.dateDepot DESC, cv.id DESC
            """)
    List<CandidatureRhDTO> toutes();

    /** Les candidatures reçues pour un recrutement (donc pour son offre). */
    @Query("""
            SELECT new com.si.rh.dto.rh.CandidatureRhDTO(
                cv.id, cd.id, cd.nom, cd.prenom, cd.email, cd.telephone, cd.experience,
                cv.nomFichier, cv.dateDepot, cv.statutLecture,
                c.id, c.statut, c.dateCandidature,
                r.id, r.reference, r.titre, p.nom)
            FROM Candidature c
            JOIN c.cv cv
            JOIN c.candidat cd
            JOIN c.recrutement r
            JOIN r.besoin b
            JOIN b.poste p
            WHERE r.id = :recrutementId
            ORDER BY c.dateCandidature DESC, c.id DESC
            """)
    List<CandidatureRhDTO> parRecrutement(@Param("recrutementId") Integer recrutementId);

    @Modifying
    @Query("UPDATE Cv cv SET cv.statutLecture = :statut WHERE cv.id = :id")
    int changerStatutLecture(@Param("id") Integer id, @Param("statut") StatutLecture statut);
}

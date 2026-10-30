package com.si.rh.dto.rh;

import com.si.rh.model.enums.StatutCandidature;
import com.si.rh.model.enums.StatutLecture;
import java.time.OffsetDateTime;

/**
 * Une ligne de la liste des CV côté RH : un CV déposé, avec la candidature associée.
 * candidatureId / statut / recrutement* sont null pour un dépôt spontané (CV sans offre).
 * Le contenu du fichier n'est jamais inclus (il se télécharge via /api/cvs/{cvId}/fichier).
 */
public record CandidatureRhDTO(
        Integer cvId,
        Integer candidatId,
        String nom,
        String prenom,
        String email,
        String telephone,
        Integer experience,
        String nomFichier,
        OffsetDateTime dateDepot,
        StatutLecture statutLecture,
        Integer candidatureId,
        StatutCandidature statut,
        OffsetDateTime dateCandidature,
        Integer recrutementId,
        String recrutementReference,
        String recrutementTitre,
        String poste) {
}

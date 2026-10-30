package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.StatutDocument;
import com.si.rh.model.enums.TypeDocument;
import java.time.OffsetDateTime;

/** Métadonnées d'un document (le contenu se télécharge via /documents/{id}/fichier). */
public record DocumentEmployeDTO(Integer id, TypeDocument typeDocument, String nomFichier, String mimeType,
        StatutDocument statut, OffsetDateTime dateDepot) {
}

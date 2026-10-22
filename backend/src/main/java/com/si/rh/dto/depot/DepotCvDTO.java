package com.si.rh.dto.depot;

import jakarta.validation.constraints.*;

/**
 * Données envoyées par le candidat avec son CV.
 * recrutementId / publicationId absents = dépôt spontané (pas de candidature créée).
 */
public record DepotCvDTO(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 30) String telephone,
        @Size(max = 30) String numeroCin,
        @PositiveOrZero Integer experience,
        String details,
        @NotBlank @Size(max = 255) String nomFichier,
        String mimeType,
        Integer recrutementId,
        Integer publicationId) {
}

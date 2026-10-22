package com.si.rh.dto.offre;

import com.si.rh.model.enums.StatutOffre;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OffreEmploiDTO(Integer id, Integer recrutementId, String titre, String descriptionPoste, String missions,
        String competencesRequises, String niveauEtude, Integer experienceMinAnnees, BigDecimal salaireMin,
        BigDecimal salaireMax, OffsetDateTime dateCreation, StatutOffre statut) {
}

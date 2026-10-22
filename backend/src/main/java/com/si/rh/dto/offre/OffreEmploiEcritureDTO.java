package com.si.rh.dto.offre;

import com.si.rh.model.enums.StatutOffre;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OffreEmploiEcritureDTO(@NotNull Integer recrutementId, @NotBlank @Size(max = 150) String titre,
        @NotBlank String descriptionPoste, @NotBlank String missions, String competencesRequises,
        @Size(max = 100) String niveauEtude, @NotNull @PositiveOrZero Integer experienceMinAnnees,
        @DecimalMin("0.0") BigDecimal salaireMin, @DecimalMin("0.0") BigDecimal salaireMax, StatutOffre statut) {
}

package com.si.rh.dto.candidature;

import com.si.rh.model.enums.StatutCandidature;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CandidatureEcritureDTO(@NotNull Integer candidatId, @NotNull Integer recrutementId, @NotNull Integer cvId,
        Integer publicationId, StatutCandidature statut, @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal score,
        String remarque) {
}

package com.si.rh.dto.candidature;

import com.si.rh.model.enums.StatutCandidature;
import jakarta.validation.constraints.NotNull;

public record CandidatureStatutDTO(
        @NotNull StatutCandidature statut
) {
}
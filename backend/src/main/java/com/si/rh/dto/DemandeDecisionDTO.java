package com.si.rh.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DemandeDecisionDTO(
        @Min(1) int idBesoin,
        @NotBlank String decisionCode,
        String remarque
) {}

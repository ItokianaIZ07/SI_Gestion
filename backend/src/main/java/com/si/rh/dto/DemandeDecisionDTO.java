package com.si.rh.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record DemandeDecisionDTO(
        @Min(1) int idBesoin,
        @NotBlank String decisionCode,
        String remarque,
        Integer idGrilleSalaire,
        BigDecimal salaire
) {}

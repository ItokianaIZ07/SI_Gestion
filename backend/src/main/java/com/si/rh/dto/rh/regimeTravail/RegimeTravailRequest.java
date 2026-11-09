package com.si.rh.dto.rh.regimeTravail;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegimeTravailRequest(
        @NotBlank(message = "Le code est obligatoire.")
        @Pattern(regexp = "^\\S+$", message = "Le code ne doit contenir aucun espace.")
        @Size(max = 50, message = "Le code ne peut pas dépasser 50 caractères.")
        String code,

        @NotBlank(message = "Le libellé est obligatoire.")
        @Size(max = 100, message = "Le libellé ne peut pas dépasser 100 caractères.")
        String libelle,

        String description,

        @NotNull(message = "Les heures hebdomadaires sont obligatoires.")
        @DecimalMin(value = "0", inclusive = false, message = "Les heures hebdomadaires doivent être supérieures à 0.")
        @DecimalMax(value = "60", message = "Les heures hebdomadaires ne peuvent pas dépasser 60.")
        BigDecimal heuresHebdomadaires) {
}

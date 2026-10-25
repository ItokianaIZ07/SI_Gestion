package com.si.rh.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GrilleSalaireCreationRequest(
        @NotNull(message = "Le salaire minimum est obligatoire")
        @PositiveOrZero(message = "Le salaire minimum ne peut pas être négatif")
        BigDecimal valeurMin,

        @NotNull(message = "Le salaire maximum est obligatoire")
        @PositiveOrZero(message = "Le salaire maximum ne peut pas être négatif")
        BigDecimal valeurMax,

        @NotNull(message = "La date de début de validité est obligatoire")
        LocalDate dateDebutValidite,

        LocalDate dateFinValidite
) {
}

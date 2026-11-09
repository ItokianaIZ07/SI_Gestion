package com.si.rh.dto.rh.jourFerie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record JourFerieRequest(
        @NotNull(message = "La date est obligatoire.")
        LocalDate dateJour,

        @NotBlank(message = "Le libellé est obligatoire.")
        @Size(max = 100, message = "Le libellé ne peut pas dépasser 100 caractères.")
        String libelle) {
}

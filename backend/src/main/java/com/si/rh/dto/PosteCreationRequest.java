package com.si.rh.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PosteCreationRequest(
        @NotBlank(message = "Le nom du poste est obligatoire")
        @Size(max = 100, message = "Le nom du poste ne peut pas dépasser 100 caractères")
        String nom,

        @NotNull(message = "La grille salariale du poste est obligatoire")
        @Valid
        GrilleSalaireCreationRequest grilleSalaire
) {
}

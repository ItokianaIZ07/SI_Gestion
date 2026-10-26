package com.si.rh.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.List;

public record DepartementRequest(
        @NotBlank(message = "Le nom du département est obligatoire")
        @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
        String nom,

        @NotNull(message = "Le manager est obligatoire")
        @Min(value = 1, message = "Le manager sélectionné est invalide")
        Integer managerId,
        
        @NotNull(message = "L’effectif maximal est obligatoire")
        @Min(value = 1, message = "L’effectif maximal doit être supérieur ou égal à 1")
        Integer effectifMax,

        @NotNull(message = "La liste des postes est obligatoire")
        List<@Valid PosteCreationRequest> postes
) {
}

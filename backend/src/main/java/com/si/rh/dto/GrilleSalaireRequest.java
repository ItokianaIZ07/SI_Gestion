package com.si.rh.dto;

// BY : Fenosoa

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Données envoyées par le formulaire de la grille salariale (salaires mensuels, en ariary). */
public record GrilleSalaireRequest(
        @NotNull(message = "Choisissez un poste.")
        Integer idPoste,

        @NotNull(message = "Renseignez le salaire minimum.")
        @PositiveOrZero(message = "Le salaire minimum ne peut pas être négatif.")
        BigDecimal valeurMin,

        @NotNull(message = "Renseignez le salaire maximum.")
        @PositiveOrZero(message = "Le salaire maximum ne peut pas être négatif.")
        BigDecimal valeurMax,

        @NotNull(message = "Renseignez la date de début de validité.")
        LocalDate dateDebutValidite,
        @NotNull(message = "Renseignez la date de fin de validité.")
        LocalDate dateFinValidite
) {}
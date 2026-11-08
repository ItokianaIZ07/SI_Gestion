package com.si.rh.dto.fiche;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ChangementAffectationRequest(
        @NotNull(message = "Le département est obligatoire") Integer idDepartement,
        @NotNull(message = "Le poste est obligatoire") Integer idPoste,
        Integer idSuperieur,
        LocalDate dateEffet,
        @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères") String motif) {
}

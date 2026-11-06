package com.si.rh.dto.rh.planningTravail;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PlanningTravailRequest(
        @NotNull(message = "Le salarié est obligatoire.")
        @Positive(message = "L'identifiant du salarié doit être positif.")
        Integer idEmploye,

        @NotNull(message = "L'horaire est obligatoire.")
        @Positive(message = "L'identifiant de l'horaire doit être positif.")
        Integer idHoraire,

        @Positive(message = "L'identifiant du régime doit être positif.")
        Integer idRegime,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate dateDebut,

        LocalDate dateFin) {

    @AssertTrue(message = "La date de fin doit être égale ou postérieure à la date de début.")
    public boolean isPeriodeValide() {
        return dateDebut == null || dateFin == null || !dateFin.isBefore(dateDebut);
    }
}

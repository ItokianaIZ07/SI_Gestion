package com.si.rh.dto.rh.horaireTravail;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.List;

public record HoraireTravailRequest(

        @NotBlank(message = "Le code est obligatoire.")
        @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères.")
        String code,

        @NotBlank(message = "Le libellé est obligatoire.")
        @Size(max = 100, message = "Le libellé ne doit pas dépasser 100 caractères.")
        String libelle,

        @NotNull(message = "L'heure de début est obligatoire.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime heureDebut,

        @NotNull(message = "L'heure de fin est obligatoire.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime heureFin,

        @JsonFormat(pattern = "HH:mm")
        LocalTime debutPause,

        @JsonFormat(pattern = "HH:mm")
        LocalTime finPause,

        @NotNull(message = "La tolérance est obligatoire.")
        @Min(value = 0, message = "La tolérance ne peut pas être négative.")
        Integer toleranceRetardMinutes,

        @NotEmpty(message = "Au moins un jour travaillé est obligatoire.")
        Short @NotNull @Min(1) @Max(7) [] joursTravail
) {

    @JsonIgnore
    @AssertTrue(message = "L'heure de fin doit être supérieure à l'heure de début.")
    public boolean isHeureFinValide() {
        if (heureDebut == null || heureFin == null) return true;
        return heureFin.isAfter(heureDebut);
    }

    @JsonIgnore
    @AssertTrue(message = "La pause doit avoir un début et une fin, ou aucun des deux.")
    public boolean isPauseComplete() {
        return (debutPause == null) == (finPause == null);
    }

    @JsonIgnore
    @AssertTrue(message = "La pause doit être dans la journée et finir après son début.")
    public boolean isPauseValide() {
        if (debutPause == null || finPause == null || heureDebut == null || heureFin == null) {
            return true;
        }
        return !debutPause.isBefore(heureDebut)
                && finPause.isAfter(debutPause)
                && !finPause.isAfter(heureFin);
    }
}
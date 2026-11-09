package com.si.rh.dto.fiche;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record DepartEmployeRequest(
        @NotNull(message = "La date de départ est obligatoire.")
        @PastOrPresent(message = "La date de départ ne peut pas être dans le futur.")
        LocalDate dateDepart) {
}

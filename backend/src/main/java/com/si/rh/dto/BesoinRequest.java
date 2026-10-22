package com.si.rh.dto;

import com.si.rh.model.enums.Priorite;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BesoinRequest(
        @NotNull Integer demandeurId,
        @NotNull Integer departementId,
        @NotNull Integer typeBesoinId,
        @NotNull Integer posteId,
        Integer employeRemplaceId,
        Integer typeContratSouhaiteId,
        @NotNull @Min(1) Integer effectif,
        @NotNull Priorite priorite,
        LocalDate dateArriveeSouhaitee,
        @Size(max = 150) String lieuTravail,
        @NotBlank String motif,
        @Size(max = 255) String experienceRequise,
        @Size(max = 100) String niveauEtude,
        String competencesRequises,
        @DecimalMin("0.00") BigDecimal salaireEstime
) {
}

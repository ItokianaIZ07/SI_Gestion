package com.si.rh.dto;

import com.si.rh.model.enums.Priorite;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BesoinRequest(
        @NotNull @Min(1) Integer demandeurId,
        @NotNull @Min(1) Integer departementId,
        @NotNull @Min(1) Integer typeBesoinId,
        @NotNull @Min(1) Integer posteId,
        @Min(0) Integer employeRemplaceId,
        @NotNull @Min(1) Integer typeContratSouhaiteId,
        @NotNull @Min(1) Integer effectif,
        @NotNull Priorite priorite,
        @NotNull @FutureOrPresent LocalDate dateArriveeSouhaitee,
        @NotBlank @Size(max = 150) String lieuTravail,
        @NotBlank String motif,
        @NotBlank @Size(max = 255) String experienceRequise,
        @NotBlank @Size(max = 100) String niveauEtude,
        @NotBlank String competencesRequises,
        @NotNull @DecimalMin("1.00") BigDecimal salaireEstime
) {
}

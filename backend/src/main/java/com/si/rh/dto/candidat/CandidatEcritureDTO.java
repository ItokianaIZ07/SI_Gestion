package com.si.rh.dto.candidat;

import jakarta.validation.constraints.*;

public record CandidatEcritureDTO(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 30) String telephone,
        @Size(max = 30) String numeroCin,
        @PositiveOrZero Integer experience) {
}

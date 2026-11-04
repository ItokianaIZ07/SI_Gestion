package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.CategorieOnboarding;
import jakarta.validation.constraints.*;

public record ModeleTacheRequest(@NotBlank @Size(max = 50) String code, @NotBlank @Size(max = 150) String libelle,
        @NotNull CategorieOnboarding categorie, @NotNull @PositiveOrZero Integer delaiJours, Boolean actif) {
}

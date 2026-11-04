package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.CategorieOnboarding;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TacheCreationRequest(@NotBlank @Size(max = 150) String libelle, CategorieOnboarding categorie,
        LocalDate dateEcheance) {
}

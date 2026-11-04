package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.StatutTache;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TacheStatutRequest(@NotNull StatutTache statut, @Size(max = 2000) String remarque) {
}

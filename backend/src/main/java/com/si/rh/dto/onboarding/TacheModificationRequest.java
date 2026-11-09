package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.StatutTache;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TacheModificationRequest(
        @NotNull StatutTache statut,
        LocalDate dateEcheance,
        @Size(max = 2000) String remarque) {
}

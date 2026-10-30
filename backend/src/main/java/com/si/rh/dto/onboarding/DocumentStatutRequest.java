package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.StatutDocument;
import jakarta.validation.constraints.NotNull;

public record DocumentStatutRequest(@NotNull StatutDocument statut) {
}

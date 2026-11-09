package com.si.rh.dto.onboarding;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record HistoriqueTacheOnboardingDTO(
        Long id,
        String ancienStatut,
        String nouveauStatut,
        LocalDate ancienneEcheance,
        LocalDate nouvelleEcheance,
        String ancienneRemarque,
        String nouvelleRemarque,
        String auteur,
        OffsetDateTime dateAction) {
}

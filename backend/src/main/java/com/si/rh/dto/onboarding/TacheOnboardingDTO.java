package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.CategorieOnboarding;
import com.si.rh.model.enums.StatutTache;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TacheOnboardingDTO(Integer id, Integer onboardingId, String libelle, CategorieOnboarding categorie,
        LocalDate dateEcheance, StatutTache statut, OffsetDateTime dateRealisation, String remarque,
        String responsableNom, boolean enRetard) {
}

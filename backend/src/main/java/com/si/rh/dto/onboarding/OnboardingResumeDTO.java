package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.StatutOnboarding;
import java.time.LocalDate;

public record OnboardingResumeDTO(Integer id, Integer employeId, String matricule, String nom, String prenom,
        String poste, String departement, LocalDate dateDebut, LocalDate dateFinPrevue, StatutOnboarding statut,
        long nbTaches, long nbFaites, long nbEnRetard) {
}

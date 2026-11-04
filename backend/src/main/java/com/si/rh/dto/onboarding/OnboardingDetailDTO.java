package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.StatutOnboarding;
import java.time.LocalDate;
import java.util.List;

public record OnboardingDetailDTO(Integer id, Integer employeId, String matricule, String nom, String prenom,
        String email, String telephone, String poste, String departement, LocalDate dateEmbauche,
        LocalDate dateDebut, LocalDate dateFinPrevue, StatutOnboarding statut, String responsableNom,
        List<ContratResumeDTO> contrats, List<TacheOnboardingDTO> taches, List<DocumentEmployeDTO> documents) {
}

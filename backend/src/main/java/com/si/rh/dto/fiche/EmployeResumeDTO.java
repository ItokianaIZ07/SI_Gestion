package com.si.rh.dto.fiche;

import com.si.rh.model.enums.StatutEmploye;

import java.time.LocalDate;

public record EmployeResumeDTO(Integer id, String matricule, String nom, String prenom, String email, String poste,
        String departement, StatutEmploye statut, LocalDate dateEmbauche, LocalDate dateDepart) {
}

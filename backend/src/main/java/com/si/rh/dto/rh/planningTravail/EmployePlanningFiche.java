package com.si.rh.dto.rh.planningTravail;

import com.si.rh.model.enums.StatutEmploye;

public record EmployePlanningFiche(
        Integer id,
        String matricule,
        String nom,
        String prenom,
        String departement,
        StatutEmploye statut) {
}

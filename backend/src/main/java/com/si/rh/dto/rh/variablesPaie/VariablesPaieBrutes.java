package com.si.rh.dto.rh.variablesPaie;

public record VariablesPaieBrutes(
        Integer idEmploye,
        String matricule,
        String nom,
        String prenom,
        String departement,
        Integer annee,
        Integer mois,
        Long minutesPrevues,
        Long minutesTravaillees,
        Long minutesRetard,
        Long nbRetards,
        Long minutesSupValidees,
        Long minutesSupAValider) {
}

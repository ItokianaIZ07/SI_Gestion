package com.si.rh.dto.rh.variablesPaie;

import java.math.BigDecimal;

public record VariablesPaieLigne(
        Integer idEmploye,
        String matricule,
        String nom,
        String prenom,
        String departement,
        Integer annee,
        Integer mois,
        String tempsPrevu,
        BigDecimal tempsPrevuDecimal,
        String tempsTravaille,
        BigDecimal tempsTravailleDecimal,
        String retard,
        BigDecimal retardDecimal,
        Long nbRetards,
        String heuresSupValidees,
        BigDecimal heuresSupValideesDecimal,
        String heuresSupAValider,
        BigDecimal heuresSupAValiderDecimal) {
}

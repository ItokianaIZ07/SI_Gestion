package com.si.rh.dto.rh.variablesPaie;

import java.math.BigDecimal;

public record VariablesPaieTotaux(
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

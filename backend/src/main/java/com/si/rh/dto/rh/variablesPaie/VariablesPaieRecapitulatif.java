package com.si.rh.dto.rh.variablesPaie;

import java.util.List;

public record VariablesPaieRecapitulatif(
        Integer annee,
        Integer mois,
        List<VariablesPaieLigne> lignes,
        VariablesPaieTotaux totaux) {
}

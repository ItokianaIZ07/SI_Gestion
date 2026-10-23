package com.si.rh.dto;

// BY : Fenosoa

import java.math.BigDecimal;
import java.util.List;

/** Réponse de GET /api/finance/budget : total entreprise + une ligne par département. */
public record BudgetResponse(Total total, List<LigneDepartement> departements) {

    public record Total(BigDecimal budget, BigDecimal masseSalariale, BigDecimal depenses, BigDecimal restant) {}

    public record LigneDepartement(
            Integer idDepartement,
            String departement,
            BigDecimal budget,
            BigDecimal masseSalariale,
            BigDecimal depenses,
            BigDecimal restant) {}
}
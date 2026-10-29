package com.si.rh.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ManagerDashboardResponse(
        Integer departementId,
        String nomDepartement,
        Integer effectifMax,
        long effectifActif,
        long effectifSuspendu,
        long effectifParti,
        long postesActifs,
        long postesInactifs,
        Map<String, Long> demandesParStatut,
        List<VisionDepartementResponse.DemandeResume> demandes,
        Budget budget
) {
    public record Budget(
            Integer annee,
            BigDecimal montant,
            BigDecimal masseSalariale,
            BigDecimal depenses,
            BigDecimal restant
    ) {
    }
}

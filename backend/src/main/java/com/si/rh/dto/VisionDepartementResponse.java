package com.si.rh.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record VisionDepartementResponse(
        Integer id,
        String nom,
        Integer effectifMax,
        long effectifActif,
        long effectifSuspendu,
        long effectifParti,
        long postesActifs,
        long postesInactifs,
        Map<String, Long> demandesParStatut,
        List<DemandeResume> demandes
) {
    public record DemandeResume(
            Integer id,
            String poste,
            Integer effectif,
            String statut,
            OffsetDateTime dateDemande
    ) {
    }
}

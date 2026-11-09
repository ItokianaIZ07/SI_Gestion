package com.si.rh.dto.conge;

import java.time.LocalDateTime;

public record AnnulationCongeResponse(
        Integer idDemande,
        String statutGlobal,
        double joursRestitues,
        LocalDateTime dateAnnulation,
        String motif
) {
}

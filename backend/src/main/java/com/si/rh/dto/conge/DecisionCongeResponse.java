package com.si.rh.dto.conge;

import java.time.LocalDateTime;

public record DecisionCongeResponse(
        Integer idDemande,
        String etapeValidee,
        String decision,
        String statutGlobal,
        String commentaire,
        LocalDateTime dateDecision
) {
}

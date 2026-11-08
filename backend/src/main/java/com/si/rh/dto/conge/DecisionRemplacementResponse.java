package com.si.rh.dto.conge;

import java.time.LocalDateTime;

public record DecisionRemplacementResponse(
        Long idRemplacement,
        Integer idDemande,
        String decision,
        String commentaire,
        LocalDateTime dateDecision
) {
}

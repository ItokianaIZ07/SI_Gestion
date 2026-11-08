package com.si.rh.dto.conge;

import java.time.LocalDate;

public record RegleCongeResponse(
        Integer idRegle,
        boolean deductionSolde,
        boolean justificatifObligatoire,
        boolean remplacantObligatoire,
        boolean acquisitionAutorisee,
        double joursAcquisMois,
        Double plafondJours,
        LocalDate dateDebutValidite,
        LocalDate dateFinValidite
) {
}

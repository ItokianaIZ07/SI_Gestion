package com.si.rh.dto;

// BY : Fenosoa

import java.math.BigDecimal;
import java.time.LocalDate;

public record GrilleSalaireResponse(
        Integer id,
        Integer idPoste,
        String poste,
        String departement,
        BigDecimal valeurMin,
        BigDecimal valeurMax,
        LocalDate dateDebutValidite,
        LocalDate dateFinValidite
) {}
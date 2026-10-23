package com.si.rh.dto;

public record DepartementResumeResponse(
        Integer id,
        String nom,
        Integer effectifMax,
        long effectifActif,
        long postes,
        long demandes
) {
}

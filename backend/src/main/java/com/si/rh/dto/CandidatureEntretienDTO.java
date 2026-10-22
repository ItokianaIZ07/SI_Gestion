package com.si.rh.dto;

public record CandidatureEntretienDTO(
    Integer id,
    String candidatNom,
    String candidatPrenom,
    String candidatEmail,
    String poste,
    String departement,
    String statut,
    EntretienDTO entretien
) {}
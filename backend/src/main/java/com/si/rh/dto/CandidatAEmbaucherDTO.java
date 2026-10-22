package com.si.rh.dto;

import java.math.BigDecimal;

public record CandidatAEmbaucherDTO(
    Integer id,
    String candidatNom,
    String candidatPrenom,
    String candidatEmail,
    String poste,
    String departement,
    BigDecimal salaireEstime,
    String typeContratSouhaite,
    Integer idPoste,
    Integer idDepartement
) {}
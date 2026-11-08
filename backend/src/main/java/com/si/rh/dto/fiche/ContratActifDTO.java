package com.si.rh.dto.fiche;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratActifDTO(Integer id, String reference, String typeContrat, String statut, LocalDate dateDebut,
        LocalDate dateFin, LocalDate dateSignature, BigDecimal salaire, Integer periodeEssaiJours) {
}

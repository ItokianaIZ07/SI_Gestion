package com.si.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ContratDTO(
    Integer id,
    String reference,
    Integer idEmploye,
    String employeNom,
    String employePrenom,
    String typeContrat,
    String statut,
    LocalDate dateDebut,
    LocalDate dateFin,
    LocalDate dateSignature,
    BigDecimal salaire,
    OffsetDateTime dateCreation,
    Integer nbSignatures   // ← AJOUT
) {}
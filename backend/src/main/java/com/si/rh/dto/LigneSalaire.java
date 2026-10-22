package com.si.rh.dto;

// BY : Fenosoa
// Extrait léger d'un contrat (sans le PDF) pour calculer la masse salariale.

import java.math.BigDecimal;
import java.time.LocalDate;

public record LigneSalaire(Integer idDepartement, BigDecimal salaire, LocalDate dateDebut, LocalDate dateFin) {}
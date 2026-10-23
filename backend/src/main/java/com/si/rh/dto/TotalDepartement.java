package com.si.rh.dto;

// BY : Fenosoa
// Ligne (département, montant) renvoyée par les requêtes de calcul du budget.

import java.math.BigDecimal;

public record TotalDepartement(Integer idDepartement, BigDecimal total) {}
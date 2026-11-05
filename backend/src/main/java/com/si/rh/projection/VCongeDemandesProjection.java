package com.si.rh.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface VCongeDemandesProjection {

    Integer getIdDemande();

    Integer getIdEmploye();

    Integer getIdTypeConge();

    String getCodeTypeConge();

    String getTypeConge();

    LocalDate getDateDebut();

    LocalDate getDateFin();

    double getNombreJours();

    String getMotif();

    LocalDateTime getDateDemande(); // Type TIMESTAMP -> LocalDateTime

    String getStatutGlobal();

    LocalDateTime getDateDecisionFinale(); // Type TIMESTAMP -> LocalDateTime
}

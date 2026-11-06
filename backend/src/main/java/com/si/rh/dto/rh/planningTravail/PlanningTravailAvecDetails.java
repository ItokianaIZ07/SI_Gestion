package com.si.rh.dto.rh.planningTravail;

import java.time.LocalDate;

public interface PlanningTravailAvecDetails {
    Integer getId();
    Integer getIdEmploye();
    Integer getIdDepartement();
    String getMatricule();
    String getNom();
    String getPrenom();
    String getDepartement();
    String getHoraire();
    String getRegime();
    LocalDate getDateDebut();
    LocalDate getDateFin();
    Boolean getEnCours();
}

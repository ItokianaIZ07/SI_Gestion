package com.si.rh.dto.rh.horaireTravail;

import java.time.LocalTime;

public interface HorarireAvecUsage {
    Integer getId();
    String getCode();
    String getLibelle();
    LocalTime getHeureDebut();
    LocalTime getHeureFin();
    LocalTime getDebutPause();
    LocalTime getFinPause();
    Integer getToleranceRetardMinutes();
    Short[] getJoursTravail();
    Boolean getActif();
    Integer getMinutesPrevues();
    Long getNbPlannings();
}
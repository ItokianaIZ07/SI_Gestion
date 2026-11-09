package com.si.rh.dto.rh.jourFerie;

import java.time.LocalDate;

public interface JourFerieAvecJourSemaine {
    LocalDate getDateJour();
    String getLibelle();
    Integer getJourSemaine();
}

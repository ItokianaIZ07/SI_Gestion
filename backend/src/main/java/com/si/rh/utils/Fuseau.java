package com.si.rh.utils;

import java.time.LocalDate;
import java.time.ZoneId;

/** Fuseau de l'entreprise (Madagascar, UTC+3) : toutes les dates « du jour » en dépendent. */
public final class Fuseau {

    public static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");

    private Fuseau() {
    }

    public static LocalDate aujourdhui() {
        return LocalDate.now(MADAGASCAR);
    }
}

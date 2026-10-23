package com.si.rh.service;

// BY : Fenosoa

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Période de validité d'une grille, bornes incluses.
 * fin == null : période sans date de fin.
 * Même règle que la contrainte SQL ex_grille_periode : daterange(debut, fin, '[]').
 */
public record PeriodeValidite(LocalDate debut, LocalDate fin) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PeriodeValidite {
        if (debut == null) {
            throw new IllegalArgumentException("La date de début est obligatoire.");
        }
        if (fin != null && fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit suivre la date de début.");
        }
    }

    public boolean seChevauche(PeriodeValidite autre) {
        boolean finApresDebutAutre = fin == null || !fin.isBefore(autre.debut);
        boolean autreFinApresMonDebut = autre.fin == null || !autre.fin.isBefore(debut);
        return finApresDebutAutre && autreFinApresMonDebut;
    }

    @Override
    public String toString() {
        return fin == null
                ? "à partir du " + debut.format(FORMAT)
                : "du " + debut.format(FORMAT) + " au " + fin.format(FORMAT);
    }
}
package com.si.rh.exception;

import java.sql.SQLException;

public final class MessageBase {

    private static final String ETAT_RAISE_EXCEPTION = "P0001";

    private MessageBase() {
    }

    public static String regleMetier(Throwable erreur) {
        Throwable t = erreur;
        for (int i = 0; t != null && i < 20; i++) {
            if (t instanceof SQLException sql && ETAT_RAISE_EXCEPTION.equals(sql.getSQLState())) {
                return nettoyer(sql.getMessage());
            }
            if (t.getCause() == t) {
                break;
            }
            t = t.getCause();
        }
        return null;
    }

    static String nettoyer(String message) {
        if (message == null || message.isBlank()) {
            return "Opération refusée par une règle métier de la base de données.";
        }
        String premiere = message.strip().split("\\R", 2)[0];
        return premiere.replaceFirst("^(?i)(ERROR|ERREUR)\\s*:\\s*", "").strip();
    }
}

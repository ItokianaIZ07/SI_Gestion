package com.si.rh.exception;

// BY : Fenosoa

import org.springframework.http.HttpStatus;

/**
 * Erreur métier à renvoyer au front avec un message lisible
 * (le front affiche le champ "message" de la réponse).
 */
public class RegleMetierException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus statut;

    public RegleMetierException(HttpStatus statut, String message) {
        super(message);
        this.statut = statut;
    }

    public HttpStatus getStatut() {
        return statut;
    }
}

package com.si.rh.dto;

// BY : Fenosoa

import java.math.BigDecimal;

/**
 * Une ligne de la LISTE des demandes (volontairement légère).
 * Le détail complet est dans {@link DemandeFinanceDetailResponse}.
 * coutTotal vaut null tant que les RH n'ont pas défini de salaire estimé.
 */
public record DemandeFinanceResponse(
        Integer id,
        String poste,
        String departement,
        Integer effectif,
        BigDecimal coutTotal,
        boolean couvert,
        Etapes etapes,
        String statut
) {
    /** Décision de chaque étape : EN_ATTENTE, ACCEPTE ou REFUSE. */
    public record Etapes(String rh, String finance, String direction) {}
}
package com.si.rh.dto;

// BY : Fenosoa

import java.math.BigDecimal;

/** Une ligne du tableau « Demandes » du module Finance. */
public record DemandeFinanceResponse(
        Integer id,
        String poste,
        String departement,
        Integer effectif,
        BigDecimal salaireEstime,
        int nbMois,
        BigDecimal coutTotal,
        BigDecimal budgetRestant,
        boolean couvert,
        Etapes etapes,
        String statut,
        boolean peutValider,
        boolean peutRefuser,
        String motifBlocage
) {
    /** Décision de chaque étape : EN_ATTENTE, ACCEPTE ou REFUSE. */
    public record Etapes(String rh, String finance, String direction) {}
}
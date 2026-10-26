package com.si.rh.dto;

// BY : Fenosoa

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Page de détail d'une demande côté Finance : informations, analyse budgétaire, grille, parcours, droits. */
public record DemandeFinanceDetailResponse(
        Integer id,
        String poste,
        String departement,
        String statut,
        String priorite,

        // Informations générales
        String demandeur,
        String typeBesoin,
        Integer effectif,
        String typeContrat,
        String lieuTravail,
        OffsetDateTime dateDemande,
        LocalDate dateArrivee,

        // Motif et profil
        String motif,
        String experience,
        String niveauEtude,
        String competences,

        // Coût (null tant que les RH n'ont pas défini le salaire)
        BigDecimal salaireEstime,
        int nbMois,
        BigDecimal coutTotal,

        Budget budget,
        Grille grille,
        List<Etape> etapes,

        // Droits de l'utilisateur connecté sur cette demande
        boolean peutValider,
        boolean peutRefuser,
        String motifBlocage
) {
    public record Budget(
            int annee,
            BigDecimal budget,
            BigDecimal masseSalariale,
            BigDecimal depenses,
            BigDecimal restant,
            BigDecimal restantApres,
            boolean couvert) {}

    /** definie = false s'il n'existe aucune grille en vigueur pour ce poste. */
    public record Grille(boolean definie, BigDecimal min, BigDecimal max, boolean dansGrille) {}

    /** etat : EN_ATTENTE, ACCEPTE, REFUSE ou BLOQUE (étape pas encore atteinte). */
    public record Etape(String code, String libelle, String etat,
                        String validateur, OffsetDateTime dateDecision, String remarque) {}
}
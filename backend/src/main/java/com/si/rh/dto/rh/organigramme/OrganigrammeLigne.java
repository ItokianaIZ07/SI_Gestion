package com.si.rh.dto.rh.organigramme;

public record OrganigrammeLigne(
        Integer idEmploye,
        String matricule,
        String nom,
        String prenom,
        String departement,
        String poste,
        Integer idSuperieur,
        String nomSuperieur,
        String prenomSuperieur,
        Long nbSubordonnesDirects) {
}

package com.si.rh.dto.rh.organigramme;

public record OrganigrammePersonne(
        Integer idEmploye,
        String matricule,
        String nom,
        String prenom,
        String departement,
        String poste,
        Integer niveau) {
}

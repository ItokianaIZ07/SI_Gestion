package com.si.rh.dto.rh.organigramme;

import java.util.List;

public record OrganigrammeNoeud(
        Integer idEmploye,
        String matricule,
        String nom,
        String prenom,
        String departement,
        String poste,
        Integer idSuperieur,
        Long nbSubordonnesDirects,
        boolean superieurAbsent,
        List<OrganigrammeNoeud> subordonnes) {
}

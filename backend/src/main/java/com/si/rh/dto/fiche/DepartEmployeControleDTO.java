package com.si.rh.dto.fiche;

public record DepartEmployeControleDTO(
        long nbSubordonnesDirects,
        boolean contratActif,
        long demandesCongeEnAttente,
        long missionsEnCours) {
}

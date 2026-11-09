package com.si.rh.dto.conge;

import java.time.LocalDate;

/**
 *
 * @Author Itokiana
 * */

public record DemandeCongeDTO(
        Integer idDemande,
        Integer idEmploye,
        String nomEmploye,
        String prenomEmploye,
        String matriculeEmploye,
        String emailEmploye,
        LocalDate dateDebutConge,
        LocalDate dateFinConge,
        String typeConge,
        double nombreJourConge,
        String motif,
        Integer idEmployeRemplacant
) {
}

package com.si.rh.dto.conge;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DemandeCongeEnAttenteDTO(
        Integer idDemande,
        Integer idEmploye,
        String nomEmploye,
        String prenomEmploye,
        String matriculeEmploye,
        String emailEmploye,
        LocalDate dateDebut,
        LocalDate dateFin,
        String typeConge,
        double nombreJours,
        String motif,
        String statutGlobal,
        LocalDateTime dateDemande
) {
}

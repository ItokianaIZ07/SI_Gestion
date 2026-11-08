package com.si.rh.dto.conge;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RemplacementCongeResponse(
        Long idRemplacement,
        Integer idDemande,
        Integer idEmployeDemandeur,
        String nomEmployeDemandeur,
        String prenomEmployeDemandeur,
        String matriculeEmployeDemandeur,
        String typeConge,
        LocalDate dateDebut,
        LocalDate dateFin,
        double nombreJours,
        LocalDateTime dateProposition,
        String decision,
        String commentaire,
        LocalDateTime dateDecision
) {
}

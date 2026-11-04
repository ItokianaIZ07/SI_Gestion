package com.si.rh.dto.onboarding;

import java.time.LocalDate;

/** Un contrat de l'employé (les contrats successifs sont tous conservés : CDD, renouvellement, CDI…). */
public record ContratResumeDTO(Integer id, String reference, String typeContrat, String statut,
        LocalDate dateDebut, LocalDate dateFin, LocalDate dateSignature) {
}

package com.si.rh.dto.conge;

import java.time.LocalDateTime;

public record ValidationCongeDTO(
        Integer idValidation,
        Integer idDemande,
        Integer idUtilisateur,
        String emailValidateur,
        int ordre,
        String etape,
        String decision,
        String commentaire,
        LocalDateTime dateValidation
) {
}

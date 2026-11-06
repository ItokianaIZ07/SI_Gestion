package com.si.rh.projection;

import java.time.LocalDateTime;

public interface VCongeValidationProjection {
    Integer getIdValidation();

    Integer getIdDemandeConge();

    Integer getIdUtilisateur();

    String getEmailValidateur();

    Integer getOrdre();

    String getEtapeValidation();

    String getCodeDecision();

    String getCommentaire();

    LocalDateTime getDateValidation();
}

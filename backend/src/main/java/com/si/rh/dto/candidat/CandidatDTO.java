package com.si.rh.dto.candidat;

import java.time.OffsetDateTime;

public record CandidatDTO(Integer id, String nom, String prenom, String email, String telephone, String numeroCin,
        Integer experience, OffsetDateTime dateCreation) {
}

package com.si.rh.dto.candidature;

import com.si.rh.model.enums.StatutCandidature;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CandidatureDTO(Integer id, Integer candidatId, Integer recrutementId, Integer cvId, Integer publicationId,
        OffsetDateTime dateCandidature, StatutCandidature statut, BigDecimal score, String remarque) {
}

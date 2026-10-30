package com.si.rh.dto.rh;

import com.si.rh.model.enums.StatutCandidature;

public record CandidatureStatutDTO(Integer id, StatutCandidature statut, String remarque) {
}

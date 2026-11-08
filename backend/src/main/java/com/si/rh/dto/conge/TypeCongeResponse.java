package com.si.rh.dto.conge;

public record TypeCongeResponse(
        Integer idType,
        String code,
        String libelleTypeConge,
        boolean actif,
        String description,
        RegleCongeResponse regle
) {
}

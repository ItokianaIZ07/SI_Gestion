package com.si.rh.dto.document;

import com.si.rh.model.enums.TypeDocument;

import java.time.LocalDate;

public record DocumentAlerteDTO(Integer idDocument, Integer idEmploye, String matricule, String employeNom,
        TypeDocument typeDocument, LocalDate dateExpiration, Long joursRestants, String etatValidite) {
}

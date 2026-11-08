package com.si.rh.dto.document;

import com.si.rh.model.enums.StatutDocument;
import com.si.rh.model.enums.TypeDocument;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DocumentAdminLigne(Integer id, TypeDocument typeDocument, String nomFichier, String mimeType,
        StatutDocument statut, OffsetDateTime dateDepot, LocalDate dateExpiration, LocalDate dateRenouvellement,
        Integer delaiAlerteJours) {
}

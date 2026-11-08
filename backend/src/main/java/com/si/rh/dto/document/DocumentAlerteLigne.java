package com.si.rh.dto.document;

import com.si.rh.model.enums.StatutDocument;
import com.si.rh.model.enums.TypeDocument;

import java.time.LocalDate;

public record DocumentAlerteLigne(Integer idDocument, Integer idEmploye, String matricule, String nom,
        String prenom, TypeDocument typeDocument, StatutDocument statut, LocalDate dateExpiration,
        Integer delaiAlerteJours) {
}

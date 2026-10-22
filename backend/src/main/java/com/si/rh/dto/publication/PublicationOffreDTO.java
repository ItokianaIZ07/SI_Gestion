package com.si.rh.dto.publication;

import com.si.rh.model.enums.StatutPublication;
import java.time.LocalDate;

public record PublicationOffreDTO(Integer id, Integer offreId, String plateforme, String urlPublication,
        LocalDate datePublication, LocalDate dateExpiration, StatutPublication statut) {
}

package com.si.rh.dto.besoin;

import com.si.rh.model.enums.Priorite;
import com.si.rh.model.enums.StatutGlobalBesoin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BesoinDTO(Integer id, String typeBesoinLibelle, String posteNom, String departementNom, Integer effectif,
        Priorite priorite, LocalDate dateArriveeSouhaitee, String lieuTravail, String motif, String experienceRequise,
        String niveauEtude, String competencesRequises, BigDecimal salaireEstime, StatutGlobalBesoin statutGlobal,
        OffsetDateTime dateDemande) {
}

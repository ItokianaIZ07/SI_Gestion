package com.si.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BesoinValidationDTO(
        Integer id,
        String poste,
        String departement,
        Integer idDepartement,
        String demandeur,
        String type,
        Integer effectif,
        Integer effectifTotal,
        Integer effectifMax,
        String priorite,
        String lieuTravail,
        String motif,
        String experienceRequise,
        String niveauEtude,
        String competencesRequises,
        BigDecimal salaireEstime,
        LocalDate dateArriveeSouhaitee,
        OffsetDateTime dateDemande,
        String statutGlobal,
        String decisionRh,
        String decisionFinance,
        String decisionDirection,
        boolean peutDecider,
        boolean accepterDesactive,
        boolean refuserDesactive,
        boolean estAssigneRecrutement
) {}
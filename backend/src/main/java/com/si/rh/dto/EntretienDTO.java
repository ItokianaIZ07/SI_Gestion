//Brandon
package com.si.rh.dto;

import java.time.LocalDateTime;

public record EntretienDTO(
    Integer id,
    Integer idCandidature,
    String typeEntretien,
    LocalDateTime dateEntretien,
    String lieu,
    String statut,
    Double note,
    String commentaire,
    Integer idUtilisateurIntervieweur,
    String intervieweurNom
) {}
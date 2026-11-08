package com.si.rh.dto.fiche;

import java.time.LocalDate;

public record AffectationDTO(Integer id, Integer idDepartement, String departement, Integer idPoste, String poste,
        Integer idSuperieur, String superieurNom, LocalDate dateDebut, LocalDate dateFin, String motif,
        boolean courante) {
}

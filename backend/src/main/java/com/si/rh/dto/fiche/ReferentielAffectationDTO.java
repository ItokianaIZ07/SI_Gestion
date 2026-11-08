package com.si.rh.dto.fiche;

import java.util.List;

public record ReferentielAffectationDTO(List<DepartementRef> departements, List<PosteRef> postes,
        List<EmployeRef> employes) {

    public record DepartementRef(Integer id, String nom) {
    }

    public record PosteRef(Integer id, String nom, Integer idDepartement) {
    }

    public record EmployeRef(Integer id, String nomComplet, String matricule) {
    }
}

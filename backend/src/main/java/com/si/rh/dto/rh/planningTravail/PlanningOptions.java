package com.si.rh.dto.rh.planningTravail;

import java.util.List;

public record PlanningOptions(
        List<EmployeOption> employes,
        List<DepartementOption> departements,
        List<HoraireOption> horaires,
        List<RegimeOption> regimes) {

    public record EmployeOption(Integer id, String matricule, String nom, String prenom) {
    }

    public record DepartementOption(Integer id, String nom) {
    }

    public record HoraireOption(Integer id, String code, String libelle) {
    }

    public record RegimeOption(Integer id, String code, String libelle) {
    }
}

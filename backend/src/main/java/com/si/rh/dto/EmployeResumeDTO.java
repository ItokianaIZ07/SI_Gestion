package com.si.rh.dto;

public record EmployeResumeDTO(
        Integer id,
        String matricule,
        String nom,
        String prenom,
        String email
) {
}

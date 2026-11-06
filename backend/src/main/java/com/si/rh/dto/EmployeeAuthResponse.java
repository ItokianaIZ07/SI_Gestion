package com.si.rh.dto;

public record EmployeeAuthResponse(
        boolean success,
        String message,
        String role,
        EmployeeIdentity employe
) {
    public record EmployeeIdentity(
            Integer id,
            String matricule,
            String nom,
            String prenom,
            String email
    ) {
    }
}

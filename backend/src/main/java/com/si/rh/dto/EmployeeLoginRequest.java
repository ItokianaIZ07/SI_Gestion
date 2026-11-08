package com.si.rh.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeLoginRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 30) String matricule,
        @NotBlank @Size(max = 30) String numeroCin
) {
}

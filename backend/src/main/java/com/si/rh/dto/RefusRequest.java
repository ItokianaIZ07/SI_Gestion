package com.si.rh.dto;

// BY : Fenosoa

import jakarta.validation.constraints.NotBlank;

public record RefusRequest(
        @NotBlank(message = "La remarque est obligatoire pour refuser une demande.")
        String remarque
) {}
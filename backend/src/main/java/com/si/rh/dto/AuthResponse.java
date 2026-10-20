package com.si.rh.dto;

public record AuthResponse(
        boolean success,
        String message,
        String role
) {}
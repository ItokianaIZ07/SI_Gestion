package com.si.rh.service;

// BY : Doneli

import com.si.rh.dto.AuthResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public AuthResponse creerReponse(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .findFirst().orElse(null);
        return new AuthResponse(true, "Authentification réussie", role);
    }
}
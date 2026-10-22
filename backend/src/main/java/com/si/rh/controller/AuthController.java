package com.si.rh.controller;

// BY : Doneli

import com.si.rh.dto.AuthResponse;
import com.si.rh.dto.LoginRequest;
import com.si.rh.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) { // org.springframework.security.web.csrf.CsrfToken
        return token;
    }

    @GetMapping("/me")
    public AuthResponse me(
            Authentication authentication,
            HttpServletRequest request) {

        var session = request.getSession(false);

        System.out.println("Session dans /me : "
                + (session != null ? session.getId() : "AUCUNE"));

        System.out.println("Contexte dans /me : "
                + (session != null
                ? session.getAttribute("SPRING_SECURITY_CONTEXT")
                : "AUCUN"));

        System.out.println("Authentication dans /me : "
                + authentication);

        return authService.creerReponse(authentication);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.email(),
                                    request.motDePasse()
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            if (httpRequest.getSession(false) != null) {
                httpRequest.changeSessionId();
            }
            contextRepository.saveContext(
                    context, httpRequest, httpResponse
            );

            AuthResponse response =
                    authService.creerReponse(authentication);

            return ResponseEntity.ok(response);

        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(
                            false,
                            "Email ou mot de passe incorrect",
                            null
                    ));
        } catch (Exception e) {
            throw new RuntimeException("Erreur d'authentification",e);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        SecurityContextLogoutHandler logoutHandler =
                new SecurityContextLogoutHandler();

        logoutHandler.setSecurityContextRepository(contextRepository);

        logoutHandler.logout(request, response, authentication);

        return ResponseEntity.ok(
                new AuthResponse(
                        true,
                        "Déconnexion réussie",
                        null
                )
        );
    }
}
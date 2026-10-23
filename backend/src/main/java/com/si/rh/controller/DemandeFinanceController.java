package com.si.rh.controller;

// BY : Fenosoa
// Routes sous /api/finance/demandes (réservées FINANCE et DIRECTION par SecurityConfig).

import com.si.rh.dto.DemandeFinanceResponse;
import com.si.rh.dto.RefusRequest;
import com.si.rh.service.DemandeFinanceService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance/demandes")
public class DemandeFinanceController {

    private final DemandeFinanceService demandeService;

    public DemandeFinanceController(DemandeFinanceService demandeService) {
        this.demandeService = demandeService;
    }

    @GetMapping
    public List<DemandeFinanceResponse> lister() {
        return demandeService.lister();
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<Void> valider(@PathVariable Integer id, Authentication authentication) {
        demandeService.valider(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refuser")
    public ResponseEntity<Void> refuser(@PathVariable Integer id,
                                        @Valid @RequestBody RefusRequest demande,
                                        Authentication authentication) {
        demandeService.refuser(id, authentication.getName(), demande.remarque());
        return ResponseEntity.noContent().build();
    }
}
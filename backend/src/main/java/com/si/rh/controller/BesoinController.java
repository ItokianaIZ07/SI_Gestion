package com.si.rh.controller;

import com.si.rh.dto.BesoinValidationDTO;
import com.si.rh.dto.DemandeDecisionDTO;
import com.si.rh.service.ValidationBesoinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rh/besoins")
public class BesoinController {
    private final ValidationBesoinService validationBesoinService;

    public BesoinController(ValidationBesoinService validationBesoinService) {
        this.validationBesoinService = validationBesoinService;
    }

    @GetMapping
    public List<BesoinValidationDTO> getListBesoin(Authentication authentication) {
        return validationBesoinService.getBesoinsAvecEtat(authentication.getName());
    }

    @PostMapping("/decision")
    public ResponseEntity<String> decide(
            @Valid @RequestBody DemandeDecisionDTO demandeDecision,
            Authentication authentication) {
        validationBesoinService.prendreDecision(
                demandeDecision.idBesoin(),
                demandeDecision.decisionCode(),
                demandeDecision.remarque(),
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body("Décision enregistrée.");
    }
}

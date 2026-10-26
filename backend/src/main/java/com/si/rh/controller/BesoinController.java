package com.si.rh.controller;

import com.si.rh.dto.BesoinValidationDTO;
import com.si.rh.dto.DemandeDecisionDTO;
import com.si.rh.service.ValidationBesoinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.besoin.BesoinDTO;
import com.si.rh.dto.BesoinRequest;
import org.springframework.http.ResponseEntity;

import com.si.rh.model.Besoin;
import com.si.rh.service.BesoinService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rh/besoins")
public class BesoinController {
    private final ValidationBesoinService validationBesoinService;
    private final BesoinService service;

    public BesoinController(ValidationBesoinService validationBesoinService, BesoinService service) {
        this.validationBesoinService = validationBesoinService;
        this.service = service;
    }

    @GetMapping
    public List<BesoinValidationDTO> getListBesoin(Authentication authentication) {
        return validationBesoinService.getBesoinsAvecEtat(authentication.getName());
    }

    @PostMapping("/decision")
    public ResponseEntity<String> decide(
            @Valid @RequestBody DemandeDecisionDTO demandeDecision,
            Authentication authentication) {
        System.out.println(authentication.getName());
        validationBesoinService.prendreDecision(
                demandeDecision.idBesoin(),
                demandeDecision.decisionCode(),
                demandeDecision.remarque(),
                demandeDecision.salaire(),
                demandeDecision.idGrilleSalaire(),
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body("Décision enregistrée.");
    }

    @PostMapping
    public ResponseEntity<Integer> creer(@Valid @RequestBody BesoinRequest request) {
        Besoin besoin = service.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(besoin.getId());
    }
//     @GetMapping("")
//     public List<Besoin> getListBesoin(){
//         return service.getAll();
//     }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BesoinDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste : récupération réussie", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BesoinDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Besoin récupéré avec succès", service.parId(id)));
    }
}

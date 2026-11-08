package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.fiche.*;
import com.si.rh.service.AffectationService;
import com.si.rh.service.FicheEmployeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rh/employes")
public class FicheEmployeController {

    private final FicheEmployeService ficheService;
    private final AffectationService affectationService;

    public FicheEmployeController(FicheEmployeService ficheService, AffectationService affectationService) {
        this.ficheService = ficheService;
        this.affectationService = affectationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeResumeDTO>>> liste() {
        return ResponseEntity.ok(ApiResponse.success("Liste des employés récupérée", ficheService.liste()));
    }

    @GetMapping("/referentiels-affectation")
    public ResponseEntity<ApiResponse<ReferentielAffectationDTO>> referentiels() {
        return ResponseEntity.ok(ApiResponse.success("Référentiels récupérés", affectationService.referentiels()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FicheEmployeDTO>> fiche(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Fiche salarié récupérée", ficheService.fiche(id)));
    }

    @GetMapping("/{id}/carriere")
    public ResponseEntity<ApiResponse<List<AffectationDTO>>> carriere(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Carrière récupérée", affectationService.carriere(id)));
    }

    @PostMapping("/{id}/affectations")
    public ResponseEntity<ApiResponse<List<AffectationDTO>>> changerAffectation(@PathVariable Integer id,
            @Valid @RequestBody ChangementAffectationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Affectation modifiée", affectationService.changer(id, req)));
    }
}

package com.si.rh.controller;

// BY : Fenosoa
// Routes sous /api/finance/** : réservées aux rôles FINANCE et DIRECTION (SecurityConfig).

import com.si.rh.dto.GrilleSalaireRequest;
import com.si.rh.dto.GrilleSalaireResponse;
import com.si.rh.dto.PosteResponse;
import com.si.rh.service.GrilleSalaireService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class GrilleSalaireController {

    private final GrilleSalaireService grilleService;

    public GrilleSalaireController(GrilleSalaireService grilleService) {
        this.grilleService = grilleService;
    }

    @GetMapping("/grilles-salaire")
    public List<GrilleSalaireResponse> listerGrilles() {
        return grilleService.listerGrilles();
    }

    @PostMapping("/grilles-salaire")
    public ResponseEntity<GrilleSalaireResponse> creerGrille(
            @Valid @RequestBody GrilleSalaireRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grilleService.creerGrille(demande));
    }

    @PutMapping("/grilles-salaire/{id}")
    public GrilleSalaireResponse modifierGrille(
            @PathVariable Integer id,
            @Valid @RequestBody GrilleSalaireRequest demande) {
        return grilleService.modifierGrille(id, demande);
    }

    /** Liste des postes actifs pour le formulaire. */
    @GetMapping("/postes")
    public List<PosteResponse> listerPostes() {
        return grilleService.listerPostes();
    }

    @GetMapping("/grilles-salaire/departement/{idDepartement}")
    public List<GrilleSalaireResponse> listerGrillesParDepartement(
            @PathVariable Integer idDepartement) {
        return grilleService.listerGrillesParDepartement(idDepartement);
    }
}
package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.offre.OffreEmploiDTO;
import com.si.rh.dto.offre.OffreEmploiEcritureDTO;
import com.si.rh.service.OffreEmploiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/offres-emploi")
public class OffreEmploiController {
    private final OffreEmploiService service;

    public OffreEmploiController(OffreEmploiService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OffreEmploiDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste : récupération réussie", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OffreEmploiDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Offre d’emploi récupéré avec succès", service.parId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OffreEmploiDTO>> creer(@Valid @RequestBody OffreEmploiEcritureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Offre d’emploi créé(e) avec succès", service.creer(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OffreEmploiDTO>> modifier(@PathVariable Integer id,
            @Valid @RequestBody OffreEmploiEcritureDTO dto) {
        return ResponseEntity
                .ok(ApiResponse.success("Offre d’emploi modifié(e) avec succès", service.modifier(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Integer id) {
        service.supprimer(id);
        return ResponseEntity.ok(ApiResponse.success("Offre d’emploi supprimé(e) avec succès", null));
    }
}

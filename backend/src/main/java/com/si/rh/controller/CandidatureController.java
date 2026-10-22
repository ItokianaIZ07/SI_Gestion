package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.candidature.CandidatureDTO;
import com.si.rh.dto.candidature.CandidatureEcritureDTO;
import com.si.rh.service.CandidatureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
public class CandidatureController {
    private final CandidatureService service;

    public CandidatureController(CandidatureService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CandidatureDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste : récupération réussie", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidatureDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Candidature récupéré avec succès", service.parId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CandidatureDTO>> creer(@Valid @RequestBody CandidatureEcritureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidature créé(e) avec succès", service.creer(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidatureDTO>> modifier(@PathVariable Integer id,
            @Valid @RequestBody CandidatureEcritureDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Candidature modifié(e) avec succès", service.modifier(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Integer id) {
        service.supprimer(id);
        return ResponseEntity.ok(ApiResponse.success("Candidature supprimé(e) avec succès", null));
    }
}

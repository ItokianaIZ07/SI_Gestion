package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.candidat.CandidatDTO;
import com.si.rh.dto.candidat.CandidatEcritureDTO;
import com.si.rh.service.CandidatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/candidats")
public class CandidatController {
    private final CandidatService service;

    public CandidatController(CandidatService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CandidatDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste : récupération réussie", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidatDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Candidat récupéré avec succès", service.parId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CandidatDTO>> creer(@Valid @RequestBody CandidatEcritureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidat créé(e) avec succès", service.creer(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidatDTO>> modifier(@PathVariable Integer id,
            @Valid @RequestBody CandidatEcritureDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Candidat modifié(e) avec succès", service.modifier(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Integer id) {
        service.supprimer(id);
        return ResponseEntity.ok(ApiResponse.success("Candidat supprimé(e) avec succès", null));
    }
}

package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.publication.PublicationOffreDTO;
import com.si.rh.dto.publication.PublicationOffreEcritureDTO;
import com.si.rh.service.PublicationOffreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/publications-offres")
public class PublicationOffreController {
    private final PublicationOffreService service;

    public PublicationOffreController(PublicationOffreService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicationOffreDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste : récupération réussie", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicationOffreDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Publication récupéré avec succès", service.parId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PublicationOffreDTO>> creer(@Valid @RequestBody PublicationOffreEcritureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Publication créé(e) avec succès", service.creer(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicationOffreDTO>> modifier(@PathVariable Integer id,
            @Valid @RequestBody PublicationOffreEcritureDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Publication modifié(e) avec succès", service.modifier(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Integer id) {
        service.supprimer(id);
        return ResponseEntity.ok(ApiResponse.success("Publication supprimé(e) avec succès", null));
    }
}

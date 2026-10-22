package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.cv.*;
import com.si.rh.model.Cv;
import com.si.rh.service.CvService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/cvs")
public class CvController {
    private final CvService service;

    public CvController(CvService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CvDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste des CV récupérée avec succès", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CvDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("CV récupéré avec succès", service.parId(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CvDTO>> creer(@Valid @RequestPart("metadata") CvEcritureDTO dto,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CV créé avec succès", service.creer(dto, fichier.getBytes())));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CvDTO>> modifier(@PathVariable Integer id,
            @Valid @RequestPart("metadata") CvEcritureDTO dto,
            @RequestPart(value = "fichier", required = false) MultipartFile fichier) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("CV modifié avec succès",
                service.modifier(id, dto, fichier == null ? null : fichier.getBytes())));
    }

    @GetMapping("/{id}/fichier")
    public ResponseEntity<byte[]> fichier(@PathVariable Integer id) {
        Cv cv = service.fichier(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cv.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + cv.getNomFichier().replace("\"", "") + "\"")
                .body(cv.getBytes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Integer id) {
        service.supprimer(id);
        return ResponseEntity.ok(ApiResponse.success("CV supprimé avec succès", null));
    }

    @GetMapping("/candidat/{candidatId}")
    public ResponseEntity<ApiResponse<List<CvDTO>>> parCandidat(
            @PathVariable Integer candidatId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "CV du candidat récupérés avec succès",
                        service.parCandidat(candidatId)
                )
        );
    }
}

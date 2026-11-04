package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.onboarding.*;
import com.si.rh.model.DocumentEmploye;
import com.si.rh.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Zone RH (SecurityConfig : /api/rh/** = RH, DIRECTION). */
@RestController
@RequestMapping("/api/rh/onboardings")
public class OnboardingController {
    private final OnboardingService service;

    public OnboardingController(OnboardingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OnboardingResumeDTO>>> liste() {
        return ResponseEntity.ok(ApiResponse.success("Liste des onboardings récupérée", service.liste()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OnboardingDetailDTO>> detail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Onboarding récupéré", service.detail(id)));
    }

    // ----- Tâches -----

    @PostMapping("/{id}/taches")
    public ResponseEntity<ApiResponse<TacheOnboardingDTO>> ajouterTache(@PathVariable Integer id,
            @Valid @RequestBody TacheCreationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tâche ajoutée", service.ajouterTache(id, req)));
    }

    @PatchMapping("/taches/{tacheId}/statut")
    public ResponseEntity<ApiResponse<TacheOnboardingDTO>> changerStatutTache(@PathVariable Integer tacheId,
            @Valid @RequestBody TacheStatutRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Tâche mise à jour", service.changerStatutTache(tacheId, req)));
    }

    // ----- Documents -----

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentEmployeDTO>> ajouterDocument(@PathVariable Integer id,
            @RequestParam("type") String type, @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document ajouté",
                service.ajouterDocument(id, type, fichier.getOriginalFilename(), fichier.getBytes())));
    }

    @GetMapping("/documents/{docId}/fichier")
    public ResponseEntity<byte[]> fichier(@PathVariable Integer docId) {
        DocumentEmploye d = service.fichierDocument(docId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(d.getNomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(d.getBytes());
    }

    @PatchMapping("/documents/{docId}/statut")
    public ResponseEntity<ApiResponse<DocumentEmployeDTO>> changerStatutDocument(@PathVariable Integer docId,
            @Valid @RequestBody DocumentStatutRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Document mis à jour",
                service.changerStatutDocument(docId, req.statut())));
    }

    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<ApiResponse<Void>> supprimerDocument(@PathVariable Integer docId) {
        service.supprimerDocument(docId);
        return ResponseEntity.ok(ApiResponse.success("Document supprimé", null));
    }
}

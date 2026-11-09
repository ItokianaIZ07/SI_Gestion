package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.document.DocumentAdminDTO;
import com.si.rh.dto.document.DocumentExpirationRequest;
import com.si.rh.dto.document.DocumentRenouvellementRequest;
import com.si.rh.model.DocumentEmploye;
import com.si.rh.service.DocumentAdministratifService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rh")
public class DocumentAdministratifController {

    private final DocumentAdministratifService service;

    public DocumentAdministratifController(DocumentAdministratifService service) {
        this.service = service;
    }

    @GetMapping("/employes/{id}/documents")
    public ResponseEntity<ApiResponse<List<DocumentAdminDTO>>> liste(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Documents récupérés", service.liste(id)));
    }

    @PostMapping(value = "/employes/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentAdminDTO>> deposer(@PathVariable Integer id,
            @RequestParam("type") String type, @RequestPart("fichier") MultipartFile fichier,
            @RequestParam(value = "dateExpiration", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateExpiration,
            @RequestParam(value = "delaiAlerteJours", required = false) Integer delaiAlerteJours) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document déposé",
                service.deposer(id, type, fichier.getOriginalFilename(), fichier.getBytes(), dateExpiration,
                        delaiAlerteJours)));
    }

    @PatchMapping("/documents/{docId}/expiration")
    public ResponseEntity<ApiResponse<DocumentAdminDTO>> modifierExpiration(@PathVariable Integer docId,
            @RequestBody DocumentExpirationRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Validité du document mise à jour",
                service.modifierExpiration(docId, req)));
    }

    @PatchMapping("/documents/{docId}/renouvellement")
    public ResponseEntity<ApiResponse<DocumentAdminDTO>> marquerRenouvele(@PathVariable Integer docId,
            @RequestBody(required = false) DocumentRenouvellementRequest req) {
        LocalDate date = req == null ? null : req.dateRenouvellement();
        return ResponseEntity.ok(ApiResponse.success("Document marqué comme renouvelé",
                service.marquerRenouvele(docId, date)));
    }

    @GetMapping("/documents/{docId}/fichier")
    public ResponseEntity<byte[]> fichier(@PathVariable Integer docId) {
        DocumentEmploye d = service.fichier(docId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(d.getNomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(d.getBytes());
    }
}

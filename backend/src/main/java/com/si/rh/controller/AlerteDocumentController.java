package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.document.DocumentAlerteDTO;
import com.si.rh.service.AlerteDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rh/alertes/documents")
public class AlerteDocumentController {

    private final AlerteDocumentService service;

    public AlerteDocumentController(AlerteDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentAlerteDTO>>> aRenouveler() {
        return ResponseEntity.ok(ApiResponse.success("Documents à renouveler récupérés", service.aRenouveler()));
    }

    @PostMapping("/generer")
    public ResponseEntity<ApiResponse<Integer>> generer() {
        int nb = service.generer();
        return ResponseEntity.ok(ApiResponse.success(
                nb == 0 ? "Aucune nouvelle alerte à envoyer" : nb + " notification(s) envoyée(s)", nb));
    }
}

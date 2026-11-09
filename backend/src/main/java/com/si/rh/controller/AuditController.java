package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.PageResultat;
import com.si.rh.dto.audit.AuditDTO;
import com.si.rh.dto.audit.UtilisateurAuditDTO;
import com.si.rh.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/direction/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResultat<AuditDTO>>> rechercher(
            @RequestParam(required = false) Integer utilisateur,
            @RequestParam(required = false) String table,
            @RequestParam(required = false) String enregistrement,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int taille) {
        return ResponseEntity.ok(ApiResponse.success("Journal d'audit récupéré",
                service.rechercher(utilisateur, table, enregistrement, action, du, au, page, taille)));
    }

    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<String>>> tables() {
        return ResponseEntity.ok(ApiResponse.success("Tables auditées récupérées", service.tables()));
    }

    @GetMapping("/utilisateurs")
    public ResponseEntity<ApiResponse<List<UtilisateurAuditDTO>>> utilisateurs() {
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", service.utilisateurs()));
    }
}

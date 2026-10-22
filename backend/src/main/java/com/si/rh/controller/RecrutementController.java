package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.recrutement.RecrutementDTO;
import com.si.rh.dto.recrutement.RecrutementEcritureDTO;
import com.si.rh.service.RecrutementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recrutements")
public class RecrutementController {
    private final RecrutementService service;

    public RecrutementController(RecrutementService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecrutementDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste : récupération réussie", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecrutementDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Recrutement récupéré avec succès", service.parId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecrutementDTO>> creer(@Valid @RequestBody RecrutementEcritureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recrutement créé(e) avec succès", service.creer(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecrutementDTO>> modifier(@PathVariable Integer id,
            @Valid @RequestBody RecrutementEcritureDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Recrutement modifié(e) avec succès", service.modifier(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Integer id) {
        service.supprimer(id);
        return ResponseEntity.ok(ApiResponse.success("Recrutement supprimé(e) avec succès", null));
    }
}

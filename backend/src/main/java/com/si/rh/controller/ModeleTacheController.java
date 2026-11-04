package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.onboarding.ModeleTacheDTO;
import com.si.rh.dto.onboarding.ModeleTacheRequest;
import com.si.rh.service.ModeleTacheService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rh/onboarding-modeles")
public class ModeleTacheController {
    private final ModeleTacheService service;

    public ModeleTacheController(ModeleTacheService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModeleTacheDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Modèles de tâches récupérés", service.tous()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ModeleTacheDTO>> creer(@Valid @RequestBody ModeleTacheRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Modèle créé", service.creer(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ModeleTacheDTO>> modifier(@PathVariable Integer id,
            @Valid @RequestBody ModeleTacheRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Modèle modifié", service.modifier(id, req)));
    }
}

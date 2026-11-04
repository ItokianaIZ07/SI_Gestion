package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.rh.CandidatureRhDTO;
import com.si.rh.dto.rh.CandidatureStatutDTO;
import com.si.rh.dto.rh.ChangementStatutRequest;
import com.si.rh.service.CandidatureRhService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Zone RH : liste des CV / candidatures et décisions de tri (SecurityConfig : /api/rh/** = RH, DIRECTION). */
@RestController
@RequestMapping("/api/rh")
public class CandidatureRhController {
    private final CandidatureRhService service;

    public CandidatureRhController(CandidatureRhService service) {
        this.service = service;
    }

    /** Sans paramètre : tous les CV. Avec recrutementId : les candidatures de cette offre. */
    @GetMapping("/candidatures")
    public ResponseEntity<ApiResponse<List<CandidatureRhDTO>>> liste(
            @RequestParam(value = "recrutementId", required = false) Integer recrutementId) {
        return ResponseEntity.ok(ApiResponse.success("Liste des candidatures récupérée", service.liste(recrutementId)));
    }

    @PostMapping("/candidatures/{id}/preselectionner")
    public ResponseEntity<ApiResponse<CandidatureStatutDTO>> preselectionner(@PathVariable Integer id,
            @Valid @RequestBody(required = false) ChangementStatutRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Candidature présélectionnée",
                service.preselectionner(id, body == null ? null : body.remarque())));
    }

    @PostMapping("/candidatures/{id}/rejeter")
    public ResponseEntity<ApiResponse<CandidatureStatutDTO>> rejeter(@PathVariable Integer id,
            @Valid @RequestBody(required = false) ChangementStatutRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Candidature rejetée",
                service.rejeter(id, body == null ? null : body.remarque())));
    }

    @PostMapping("/cvs/{id}/lu")
    public ResponseEntity<ApiResponse<Void>> marquerLu(@PathVariable Integer id) {
        service.marquerCvLu(id);
        return ResponseEntity.ok(ApiResponse.success("CV marqué comme lu", null));
    }
}

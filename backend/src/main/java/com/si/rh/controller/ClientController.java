package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.depot.DepotCvDTO;
import com.si.rh.dto.depot.DepotCvResultatDTO;
import com.si.rh.dto.offre.OffreEmploiDTO;
import com.si.rh.dto.publication.PublicationOffreDTO;
import com.si.rh.service.ClientService;
import com.si.rh.service.DepotCvService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

/** Endpoints publics (permitAll dans SecurityConfig) utilisés par l'espace candidat. */
@RestController
@RequestMapping("/api/client")
public class ClientController {
    private final ClientService clientService;
    private final DepotCvService depotService;

    public ClientController(ClientService clientService, DepotCvService depotService) {
        this.clientService = clientService;
        this.depotService = depotService;
    }

    @GetMapping("/publications-offres")
    public ResponseEntity<ApiResponse<List<PublicationOffreDTO>>> publications() {
        return ResponseEntity.ok(ApiResponse.success("Publications actives", clientService.publicationsActives()));
    }

    @GetMapping("/offres-emploi")
    public ResponseEntity<ApiResponse<List<OffreEmploiDTO>>> offres() {
        return ResponseEntity.ok(ApiResponse.success("Offres ouvertes", clientService.offresPubliees()));
    }

    @PostMapping(value = "/depots-cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DepotCvResultatDTO>> deposer(@Valid @RequestPart("metadata") DepotCvDTO dto,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dépôt enregistré avec succès", depotService.deposer(dto, fichier.getBytes())));
    }
}

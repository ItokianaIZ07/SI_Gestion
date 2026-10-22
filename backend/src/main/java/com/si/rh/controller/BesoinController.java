package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.besoin.BesoinDTO;
import com.si.rh.dto.BesoinRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import com.si.rh.model.Besoin;
import com.si.rh.service.BesoinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/besoins")
public class BesoinController {
    private final BesoinService service;

    public BesoinController(BesoinService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Integer> creer(@Valid @RequestBody BesoinRequest request) {
        Besoin besoin = besoinService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(besoin.getId());
    }
    @GetMapping("")
    public List<Besoin> getListBesoin(){
        return service.getAll();
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BesoinDTO>>> tous() {
        return ResponseEntity.ok(ApiResponse.success("Liste : récupération réussie", service.tous()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BesoinDTO>> parId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Besoin récupéré avec succès", service.parId(id)));
    }
}

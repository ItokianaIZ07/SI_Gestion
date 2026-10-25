package com.si.rh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

import com.si.rh.service.DepartementService;
import com.si.rh.dto.DepartementRequest;
import com.si.rh.dto.DepartementResumeResponse;
import com.si.rh.dto.VisionDepartementResponse;
import com.si.rh.model.Departement;

@RestController
@RequestMapping("/api/manager/departements")
public class DepartementController {

    private final DepartementService departementService;

    public DepartementController(DepartementService departementService) {
        this.departementService = departementService;
    }

    @GetMapping
    public List<Departement> getAll() {
        return departementService.getAll();
    }

    @PostMapping
    public ResponseEntity<Integer> creer(@Valid @RequestBody DepartementRequest request) {
        Departement departement = departementService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(departement.getId());
    }

    @GetMapping("/vision-globale")
    public List<DepartementResumeResponse> getDepartements() {
        return departementService.getTousLesDepartements();
    }

    @GetMapping("/vision-globale/{departementId}")
    public VisionDepartementResponse getVision(@PathVariable Integer departementId) {
        return departementService.getDepartement(departementId);
    }
}

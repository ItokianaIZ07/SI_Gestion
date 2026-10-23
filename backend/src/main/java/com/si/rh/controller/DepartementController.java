package com.si.rh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.si.rh.service.DepartementService;
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

    @GetMapping("/vision-globale")
    public List<DepartementResumeResponse> getDepartements() {
        return departementService.getTousLesDepartements();
    }

    @GetMapping("/vision-globale/{departementId}")
    public VisionDepartementResponse getVision(@PathVariable Integer departementId) {
        return departementService.getDepartement(departementId);
    }
}

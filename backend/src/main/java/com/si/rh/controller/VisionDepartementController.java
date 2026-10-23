package com.si.rh.controller;

import com.si.rh.dto.VisionDepartementResponse;
import com.si.rh.dto.DepartementResumeResponse;
import com.si.rh.service.VisionDepartementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/departements/vision-globale")
public class VisionDepartementController {

    private final VisionDepartementService visionDepartementService;

    public VisionDepartementController(VisionDepartementService visionDepartementService) {
        this.visionDepartementService = visionDepartementService;
    }

    @GetMapping
    public List<DepartementResumeResponse> getDepartements() {
        return visionDepartementService.getTousLesDepartements();
    }

    @GetMapping("/{departementId}")
    public VisionDepartementResponse getVision(@PathVariable Integer departementId) {
        return visionDepartementService.getDepartement(departementId);
    }
}

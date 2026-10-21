package com.si.rh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.si.rh.service.DepartementService;

@RestController
@RequestMapping("/api/departements")
public class DepartementController {

    private final DepartementService departementService;

    public DepartementController(DepartementService departementService) {
        this.departementService = departementService;
    }

    @GetMapping
    public List<DepartementDto> getAll() {
        return departementService.getAll().stream()
                .map(departement -> new DepartementDto(departement.getId(), departement.getNom()))
                .toList();
    }

    public record DepartementDto(Integer id, String nom) {
    }
}

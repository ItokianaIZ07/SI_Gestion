package com.si.rh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.si.rh.service.TypeContratService;
import com.si.rh.model.TypeContrat;
@RestController
@RequestMapping("/api/manager/types-contrat")
public class TypeContratController {

    private final TypeContratService typeContratService;

    public TypeContratController(TypeContratService typeContratService) {
        this.typeContratService = typeContratService;
    }

    @GetMapping
    public List<TypeContrat> getAll() {
        return typeContratService.getAll();
    }
}

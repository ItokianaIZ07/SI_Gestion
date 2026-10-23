package com.si.rh.controller;

import com.si.rh.model.TypeBesoin;
import com.si.rh.service.TypeBesoinService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/types-besoin")
public class TypeBesoinController {

    private final TypeBesoinService typeBesoinService;

    public TypeBesoinController(TypeBesoinService typeBesoinService) {
        this.typeBesoinService = typeBesoinService;
    }

    @GetMapping
    public List<TypeBesoin> getAll() {
        return typeBesoinService.getAll();
    }
}

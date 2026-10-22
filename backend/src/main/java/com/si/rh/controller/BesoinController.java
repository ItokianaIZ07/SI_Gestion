package com.si.rh.controller;

import java.util.List;

import com.si.rh.dto.BesoinRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.si.rh.service.BesoinService;
import com.si.rh.model.Besoin;

@RestController
@RequestMapping("/api/manager/besoins")
public class BesoinController {

    private final BesoinService besoinService;

    public BesoinController(BesoinService besoinService) {
        this.besoinService = besoinService;
    }

    @GetMapping
    public List<Besoin> getAll() {
        return besoinService.getAll();
    }

    @PostMapping
    public ResponseEntity<Integer> creer(@Valid @RequestBody BesoinRequest request) {
        Besoin besoin = besoinService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(besoin.getId());
    }
}

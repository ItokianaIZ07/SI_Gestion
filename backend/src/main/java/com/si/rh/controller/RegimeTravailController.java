package com.si.rh.controller;

import com.si.rh.dto.ApiResponse;
import com.si.rh.dto.rh.regimeTravail.RegimeAvecUsage;
import com.si.rh.dto.rh.regimeTravail.RegimeTravailRequest;
import com.si.rh.model.RegimeTravail;
import com.si.rh.service.RegimeTravailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rh/regimes-travail")
public class RegimeTravailController {

    private final RegimeTravailService regimeTravailService;

    public RegimeTravailController(RegimeTravailService regimeTravailService) {
        this.regimeTravailService = regimeTravailService;
    }

    @GetMapping
    public List<RegimeAvecUsage> lister() {
        return regimeTravailService.getAllAvecNbPlannings();
    }

    @GetMapping("/{id}")
    public RegimeTravail lire(@PathVariable Integer id) {
        return regimeTravailService.getById(id);
    }

    @PostMapping
    public RegimeTravail creer(@Valid @RequestBody RegimeTravailRequest demande) {
        return regimeTravailService.creer(demande);
    }

    @PutMapping("/{id}")
    public RegimeTravail modifier(
            @PathVariable Integer id,
            @Valid @RequestBody RegimeTravailRequest demande) {
        return regimeTravailService.modifier(id, demande);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Integer id) {
        regimeTravailService.supprimer(id);
    }
}

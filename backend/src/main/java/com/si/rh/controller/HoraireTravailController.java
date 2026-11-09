package com.si.rh.controller;

import com.si.rh.dto.rh.horaireTravail.HoraireTravailRequest;
import com.si.rh.dto.rh.horaireTravail.HorarireAvecUsage;
import com.si.rh.model.HoraireTravail;
import com.si.rh.service.HoraireTravailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rh/horaires-travail")
public class HoraireTravailController {

    private final HoraireTravailService horaireTravailService;

    public HoraireTravailController(HoraireTravailService horaireTravailService) {
        this.horaireTravailService = horaireTravailService;
    }

    @GetMapping
    public List<HorarireAvecUsage> lister() {
        return horaireTravailService.getHorarireAvecUsages();
    }

    @GetMapping("/{id}")
    public HoraireTravail lire(@PathVariable Integer id) {
        return horaireTravailService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoraireTravail creer(@Valid @RequestBody HoraireTravailRequest demande) {
        return horaireTravailService.save(demande);
    }

    @PutMapping("/{id}")
    public HoraireTravail modifier(
            @PathVariable Integer id,
            @Valid @RequestBody HoraireTravailRequest demande) {
        return horaireTravailService.modifier(id, demande);
    }

    @PatchMapping("/{id}/desactiver")
    public HoraireTravail desactiver(@PathVariable Integer id) {
        return horaireTravailService.desactiver(id);
    }

    @PatchMapping("/{id}/reactiver")
    public HoraireTravail reactiver(@PathVariable Integer id) {
        return horaireTravailService.reactiver(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Integer id) {
        horaireTravailService.deleteById(id);
    }
}

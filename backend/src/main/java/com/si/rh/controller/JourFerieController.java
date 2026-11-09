package com.si.rh.controller;

import com.si.rh.dto.rh.jourFerie.JourFerieAvecJourSemaine;
import com.si.rh.dto.rh.jourFerie.JourFerieRequest;
import com.si.rh.model.JourFerie;
import com.si.rh.service.JourFerieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rh/jours-feries")
public class JourFerieController {

    private final JourFerieService jourFerieService;

    public JourFerieController(JourFerieService jourFerieService) {
        this.jourFerieService = jourFerieService;
    }

    @GetMapping
    public List<JourFerieAvecJourSemaine> lister(
            @RequestParam(required = false) Integer annee) {
        return jourFerieService.listerParAnnee(annee == null ? Year.now().getValue() : annee);
    }

    @GetMapping("/annees")
    public List<Integer> listerAnnees() {
        return jourFerieService.listerAnneesDisponibles();
    }

    @GetMapping("/{dateJour}/existe")
    public Map<String, Boolean> estFerie(@PathVariable LocalDate dateJour) {
        return Map.of("ferie", jourFerieService.estFerie(dateJour));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JourFerie ajouter(@Valid @RequestBody JourFerieRequest demande) {
        return jourFerieService.ajouter(demande);
    }

    @PutMapping("/{dateJour}")
    public JourFerie modifier(
            @PathVariable LocalDate dateJour,
            @Valid @RequestBody JourFerieRequest demande) {
        return jourFerieService.modifier(dateJour, demande);
    }

    @DeleteMapping("/{dateJour}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable LocalDate dateJour) {
        jourFerieService.supprimer(dateJour);
    }
}

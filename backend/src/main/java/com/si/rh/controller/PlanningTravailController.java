package com.si.rh.controller;

import com.si.rh.dto.rh.planningTravail.EmployePlanningFiche;
import com.si.rh.dto.rh.planningTravail.PlanningApercu;
import com.si.rh.dto.rh.planningTravail.PlanningOptions;
import com.si.rh.dto.rh.planningTravail.PlanningTravailAvecDetails;
import com.si.rh.dto.rh.planningTravail.PlanningTravailRequest;
import com.si.rh.model.PlanningTravail;
import com.si.rh.service.PlanningTravailService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import java.util.List;

@RestController
@RequestMapping("/api/rh/plannings")
public class PlanningTravailController {

    private final PlanningTravailService planningTravailService;

    public PlanningTravailController(PlanningTravailService planningTravailService) {
        this.planningTravailService = planningTravailService;
    }

    @GetMapping
    public List<PlanningTravailAvecDetails> lister(
            @RequestParam(required = false) Integer idEmploye,
            @RequestParam(required = false) Integer idDepartement,
            @RequestParam(required = false) Integer idHoraire,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return planningTravailService.lister(idEmploye, idDepartement, dateDebut, dateFin, idHoraire);
    }

    @GetMapping("/options")
    public PlanningOptions options() {
        return planningTravailService.options();
    }

    @GetMapping("/apercu")
    public PlanningApercu apercu(
            @RequestParam Integer idEmploye,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut) {
        return planningTravailService.apercuAffectation(idEmploye, dateDebut);
    }

    @GetMapping("/employes/{idEmploye}")
    public EmployePlanningFiche ficheEmploye(@PathVariable Integer idEmploye) {
        return planningTravailService.ficheEmploye(idEmploye);
    }

    @GetMapping("/employes/{idEmploye}/historique")
    public List<PlanningTravailAvecDetails> historique(@PathVariable Integer idEmploye) {
        return planningTravailService.historique(idEmploye);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanningTravail affecter(@Valid @RequestBody PlanningTravailRequest demande) {
        return planningTravailService.affecter(demande);
    }

    @PutMapping("/{id}")
    public PlanningTravail modifier(
            @PathVariable Integer id,
            @Valid @RequestBody PlanningTravailRequest demande) {
        return planningTravailService.modifier(id, demande);
    }
}

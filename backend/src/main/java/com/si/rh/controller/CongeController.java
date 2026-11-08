package com.si.rh.controller;

import com.si.rh.dto.conge.DemandeCongeDTO;
import com.si.rh.dto.conge.DemandeCongeRequest;
import com.si.rh.dto.conge.AnnulationCongeRequest;
import com.si.rh.dto.conge.AnnulationCongeResponse;
import com.si.rh.dto.conge.DecisionCongeRequest;
import com.si.rh.dto.conge.DecisionCongeResponse;
import com.si.rh.dto.conge.DemandeCongeEnAttenteDTO;
import com.si.rh.dto.conge.DecisionRemplacementRequest;
import com.si.rh.dto.conge.DecisionRemplacementResponse;
import com.si.rh.dto.conge.RemplacementCongeResponse;
import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.dto.conge.TypeCongeResponse;
import com.si.rh.dto.conge.ValidationCongeDTO;
import com.si.rh.model.DemandeConge;
import com.si.rh.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conge")
public class CongeController {

    @Autowired
    private SoldeCongeService soldeCongeService;

    @Autowired
    private DemandeCongeService demandeCongeService;

    @Autowired
    private ValidationCongeService validationCongeService;

    @Autowired
    private AnnulationCongeService annulationCongeService;

    @Autowired
    private TypeCongeService typeCongeService;

    @Autowired
    private RemplacementCongeService remplacementCongeService;

    @GetMapping("/soldes")
    public List<SoldeCongeEmployeDTO> getSoldesConge(){
        try{
            return soldeCongeService.getSoldes();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/demandes")
    public List<DemandeCongeDTO> getDemandesConge(){
        try{
            return demandeCongeService.getDemandesConge(null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/demandes/{id}")
    public List<DemandeCongeDTO> getDemandesCongeParEmploye(@PathVariable("id") int id){
        try{
            return demandeCongeService.getDemandesConge(id);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/demandes")
    public DemandeCongeDTO saveDemandeConge(@RequestBody DemandeCongeRequest demande){
        DemandeConge demandeConge = demandeCongeService.save(demande);
        return new DemandeCongeDTO(
                demandeConge.getId(),
                demandeConge.getEmploye().getId(),
                demandeConge.getEmploye().getNom(),
                demandeConge.getEmploye().getPrenom(),
                demandeConge.getEmploye().getMatricule(),
                demandeConge.getEmploye().getEmail(),
                demandeConge.getDateDebut(),
                demandeConge.getDateFin(),
                demandeConge.getTypeConge().getLibelle(),
                demandeConge.getNombreJours(),
                demandeConge.getMotif(),
                demande.getIdEmployeRemplacant());
    }

    @GetMapping("/demandes/en-attente")
    public List<DemandeCongeEnAttenteDTO> getDemandesEnAttente(
            Authentication authentication) {
        return validationCongeService.getDemandesEnAttente(authentication);
    }

    @PostMapping("/demandes/{id}/validation")
    public DecisionCongeResponse deciderDemande(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionCongeRequest request,
            Authentication authentication) {
        return validationCongeService.decider(id, request, authentication);
    }

    @GetMapping("/demandes/{id}/validations")
    public List<ValidationCongeDTO> getHistoriqueValidations(
            @PathVariable Integer id,
            Authentication authentication) {
        return validationCongeService.getHistorique(id, authentication);
    }

    @PostMapping("/demandes/{id}/annulation")
    public AnnulationCongeResponse annulerDemande(
            @PathVariable Integer id,
            @Valid @RequestBody AnnulationCongeRequest request,
            Authentication authentication) {
        return annulationCongeService.annuler(id, request, authentication);
    }

    @GetMapping("/type-conge")
    public List<TypeCongeResponse> getListTypeConge() {
        return typeCongeService.getAllType();
    }

    @GetMapping("/remplacements/moi")
    public List<RemplacementCongeResponse> getMesRemplacements(
            Authentication authentication) {
        return remplacementCongeService.getMesRemplacements(authentication);
    }

    @PostMapping("/remplacements/{id}/decision")
    public DecisionRemplacementResponse deciderRemplacement(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRemplacementRequest request,
            Authentication authentication) {
        return remplacementCongeService.decider(id, request, authentication);
    }
}

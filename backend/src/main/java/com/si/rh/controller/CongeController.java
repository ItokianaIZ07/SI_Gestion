package com.si.rh.controller;

import com.si.rh.dto.conge.DemandeCongeDTO;
import com.si.rh.dto.conge.DemandeCongeRequest;
import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.model.DemandeConge;
import com.si.rh.service.DemandeCongeService;
import com.si.rh.service.SoldeCongeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conge")
public class CongeController {

    @Autowired
    private SoldeCongeService soldeCongeService;

    @Autowired
    private DemandeCongeService demandeCongeService;

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
        try{
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
                    demandeConge.getMotif()
            );
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

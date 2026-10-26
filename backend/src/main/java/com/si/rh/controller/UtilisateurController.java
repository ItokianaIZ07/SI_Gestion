package com.si.rh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.si.rh.service.UtilisateurService;
import com.si.rh.model.Utilisateur;
@RestController
@RequestMapping("/api/manager/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public List<Utilisateur> getAll() {
        return utilisateurService.getAll();
    }

    @GetMapping("/{role}")
    public Utilisateur getByRole(@PathVariable String role){
        return utilisateurService.getByRole(role).get(0);
    }
}

package com.si.rh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.si.rh.service.PosteService;
import com.si.rh.model.Poste;
@RestController
@RequestMapping("/api/manager/postes")
public class PosteController {

    private final PosteService posteService;

    public PosteController(PosteService posteService) {
        this.posteService = posteService;
    }

    @GetMapping
    public List<Poste> getAll() {
        return posteService.getAll();
    }
}

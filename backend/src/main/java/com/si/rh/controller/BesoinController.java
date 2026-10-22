package com.si.rh.controller;

import com.si.rh.model.Besoin;
import com.si.rh.service.BesoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *@Author Itokiana
 * */
@RestController
@RequestMapping("/api/rh/besoins")
public class BesoinController {

    private final BesoinService besoinService;

    public BesoinController(BesoinService besoinService) {
        this.besoinService = besoinService;
    }

    @GetMapping("")
    public List<Besoin> getListBesoin(){
        return besoinService.getAll();
    }
}

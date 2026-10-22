package com.si.rh.controller;

import com.si.rh.dto.BesoinRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
@RequestMapping("/api")
public class BesoinController {

    private final BesoinService besoinService;

    public BesoinController(BesoinService besoinService) {
        this.besoinService = besoinService;
    }


    @PostMapping("/manager/besoins")
    public ResponseEntity<Integer> creer(@Valid @RequestBody BesoinRequest request) {
        Besoin besoin = besoinService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(besoin.getId());
    }
    @GetMapping("/manager/besoins")
    public List<Besoin> getAll(){
        return besoinService.getAll();
    }
    @GetMapping("/rh/besoins")
    public List<Besoin> getListBesoin(){
        return besoinService.getAll();
    }
}

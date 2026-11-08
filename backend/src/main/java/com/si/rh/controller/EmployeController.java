package com.si.rh.controller;

import java.util.List;

import com.si.rh.dto.EmployeResumeDTO;
import com.si.rh.dto.EmployesParDepartementDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.si.rh.service.EmployeService;
import com.si.rh.model.Employe;

@RestController
@RequestMapping("/api/manager/employes")
public class EmployeController {

    private final EmployeService employeService;

    public EmployeController(EmployeService employeService) {
        this.employeService = employeService;
    }

    @GetMapping
    public List<Employe> getAll() {
        return employeService.getAll();
    }

    @GetMapping("/par-departement")
    public List<EmployesParDepartementDTO> getEmployesParDepartement() {
        return employeService.getEmployesParDepartement();
    }

    @GetMapping("/departement/{id}/employes")
    public List<EmployeResumeDTO> getEmployesParDepartement(
            @PathVariable Integer id) {
        return employeService.getEmployesActifsParDepartement(id);
    }
}

package com.si.rh.service;

import com.si.rh.model.Employe;
import com.si.rh.repository.EmployeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeService {

    private final EmployeRepository employeRepository;

    public EmployeService(EmployeRepository employeRepository) {
        this.employeRepository = employeRepository;
    }

    public List<Employe> getAll() {
        return employeRepository.findAll();
    }
}


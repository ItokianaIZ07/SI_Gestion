package com.si.rh.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.si.rh.model.Departement;
import com.si.rh.repository.DepartementRepository;

@Service 
public class DepartementService {

    @Autowired 
    private DepartementRepository departementRepository;

    public List<Departement> getAll() {
        return departementRepository.findAll();
    }
    
}

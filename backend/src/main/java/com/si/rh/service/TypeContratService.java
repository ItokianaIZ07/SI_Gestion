package com.si.rh.service;

import com.si.rh.model.TypeContrat;
import com.si.rh.repository.TypeContratRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TypeContratService {

    private final TypeContratRepository typeContratRepository;

    public TypeContratService(TypeContratRepository typeContratRepository) {
        this.typeContratRepository = typeContratRepository;
    }

    public List<TypeContrat> getAll() {
        return typeContratRepository.findAll();
    }
}


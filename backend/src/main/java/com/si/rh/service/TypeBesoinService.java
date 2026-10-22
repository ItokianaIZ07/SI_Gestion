package com.si.rh.service;

import com.si.rh.model.TypeBesoin;
import com.si.rh.repository.TypeBesoinRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TypeBesoinService {

    private final TypeBesoinRepository typeBesoinRepository;

    public TypeBesoinService(TypeBesoinRepository typeBesoinRepository) {
        this.typeBesoinRepository = typeBesoinRepository;
    }

    public List<TypeBesoin> getAll() {
        return typeBesoinRepository.findAllByOrderByLibelleAsc();
    }
}


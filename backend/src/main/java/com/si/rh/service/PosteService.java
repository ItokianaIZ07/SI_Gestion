package com.si.rh.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.si.rh.model.Poste;
import com.si.rh.repository.PosteRepository;

@Service 
public class PosteService {

    @Autowired 
    private PosteRepository posteRepository;

    public List<Poste> getAll() {
        return posteRepository.findAll();
    }
    
}

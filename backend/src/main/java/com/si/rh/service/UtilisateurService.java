package com.si.rh.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.si.rh.model.Utilisateur;
import com.si.rh.repository.UtilisateurRepository;

@Service 
public class UtilisateurService {

    @Autowired 
    private UtilisateurRepository UtilisateurRepository;

    public List<Utilisateur> getAll() {
        return UtilisateurRepository.findAll();
    }
    
}

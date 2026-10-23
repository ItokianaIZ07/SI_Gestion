package com.si.rh.service;

import com.si.rh.model.Decision;
import com.si.rh.model.EtatValidation;
import com.si.rh.repository.EtatValidationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class EtatValidationService {
    @Autowired
    private EtatValidationRepository etatValidationRepository;

    public EtatValidation getByCode(String code){
        Optional<EtatValidation> etat =  etatValidationRepository.findByCode(code);

        if(etat.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Étape de validation inconnue.");
        }
        return etat.get();
    }
}

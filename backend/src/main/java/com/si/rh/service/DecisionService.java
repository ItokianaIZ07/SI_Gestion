package com.si.rh.service;

import com.si.rh.model.Decision;
import com.si.rh.repository.DecisionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 *
 * @Author Itokiana
 * */

@Service
public class DecisionService {
    @Autowired
    private DecisionRepository decisionRepository;

    public Decision getByCode(String code) {
        Optional<Decision> decision =  decisionRepository.findByCode(code);

        if(decision.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Décision inconnue.");
        }
        return decision.get();
    }
}

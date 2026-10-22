package com.si.rh.service;

import com.si.rh.model.Besoin;
import com.si.rh.repository.BesoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * @Author Itokiana
 */

@Service
public class BesoinService {
    @Autowired
    private BesoinRepository besoinRepository;

    public List<Besoin> getAll(){
        return besoinRepository.findAll();
    }

    public Besoin getById(int id){
        return besoinRepository.getReferenceById(id);
    }

    public Besoin save(Besoin besoin){
        return besoinRepository.save(besoin);
    }
}

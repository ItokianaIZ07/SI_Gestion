package com.si.rh.service;

import com.si.rh.model.JourFerie;
import com.si.rh.repository.JourFerieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class JourFerieService {

    private final JourFerieRepository jourFerieRepository;

    public JourFerieService(JourFerieRepository jourFerieRepository) {
        this.jourFerieRepository = jourFerieRepository;
    }

    @Transactional(readOnly = true)
    public List<JourFerie> getAll() {
        return jourFerieRepository.findAllByOrderByDateJourAsc();
    }

    @Transactional(readOnly = true)
    public Optional<JourFerie> getByDate(LocalDate dateJour) {
        return jourFerieRepository.findById(dateJour);
    }

    @Transactional
    public JourFerie save(JourFerie jourFerie) {
        return jourFerieRepository.save(jourFerie);
    }

    @Transactional
    public void deleteByDate(LocalDate dateJour) {
        jourFerieRepository.deleteById(dateJour);
    }
}

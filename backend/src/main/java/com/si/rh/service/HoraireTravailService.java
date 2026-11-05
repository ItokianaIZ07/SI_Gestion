package com.si.rh.service;

import com.si.rh.model.HoraireTravail;
import com.si.rh.repository.HoraireTravailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HoraireTravailService {

    private final HoraireTravailRepository horaireTravailRepository;

    public HoraireTravailService(HoraireTravailRepository horaireTravailRepository) {
        this.horaireTravailRepository = horaireTravailRepository;
    }

    @Transactional(readOnly = true)
    public List<HoraireTravail> getAll() {
        return horaireTravailRepository.findAllByOrderByLibelleAsc();
    }

    @Transactional(readOnly = true)
    public Optional<HoraireTravail> getById(Integer id) {
        return horaireTravailRepository.findById(id);
    }

    @Transactional
    public HoraireTravail save(HoraireTravail horaireTravail) {
        return horaireTravailRepository.save(horaireTravail);
    }

    @Transactional
    public void deleteById(Integer id) {
        horaireTravailRepository.deleteById(id);
    }
}

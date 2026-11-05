package com.si.rh.service;

import com.si.rh.model.PlanningTravail;
import com.si.rh.repository.PlanningTravailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PlanningTravailService {

    private final PlanningTravailRepository planningTravailRepository;

    public PlanningTravailService(PlanningTravailRepository planningTravailRepository) {
        this.planningTravailRepository = planningTravailRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanningTravail> getAll() {
        return planningTravailRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PlanningTravail> getById(Integer id) {
        return planningTravailRepository.findById(id);
    }

    @Transactional
    public PlanningTravail save(PlanningTravail planningTravail) {
        return planningTravailRepository.save(planningTravail);
    }

    @Transactional
    public void deleteById(Integer id) {
        planningTravailRepository.deleteById(id);
    }
}

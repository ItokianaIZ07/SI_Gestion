package com.si.rh.service;

import com.si.rh.dto.EmployeResumeDTO;
import com.si.rh.dto.EmployesParDepartementDTO;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Departement;
import com.si.rh.model.Employe;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.EmployeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final DepartementRepository departementRepository;

    public EmployeService(
            EmployeRepository employeRepository,
            DepartementRepository departementRepository) {
        this.employeRepository = employeRepository;
        this.departementRepository = departementRepository;
    }

    public List<Employe> getAll() {
        return employeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<EmployesParDepartementDTO> getEmployesParDepartement() {
        List<Departement> departements = departementRepository.findAllByOrderByNomAsc();
        Map<Integer, List<EmployeResumeDTO>> employesParDepartement = new HashMap<>();
        for (Employe employe :
                employeRepository.findByStatutOrderByNomAscPrenomAsc(StatutEmploye.ACTIF)) {
            employesParDepartement.computeIfAbsent(
                    employe.getDepartement().getId(),
                    ignored -> new ArrayList<>())
                    .add(toResume(employe));
        }

        return departements.stream()
                .map(departement -> new EmployesParDepartementDTO(
                        departement.getId(),
                        departement.getNom(),
                        employesParDepartement.getOrDefault(
                                departement.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeResumeDTO> getEmployesActifsParDepartement(Integer departementId) {
        if (!departementRepository.existsById(departementId)) {
            throw new RessourceIntrouvableException(
                    "Département introuvable : " + departementId);
        }
        return employeRepository
                .findByDepartement_IdAndStatutOrderByNomAscPrenomAsc(
                        departementId, StatutEmploye.ACTIF)
                .stream()
                .map(this::toResume)
                .toList();
    }

    private EmployeResumeDTO toResume(Employe employe) {
        return new EmployeResumeDTO(
                employe.getId(),
                employe.getMatricule(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail());
    }
}

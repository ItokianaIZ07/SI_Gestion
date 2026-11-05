package com.si.rh.service;

import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.model.Employe;
import com.si.rh.model.TypeConge;
import com.si.rh.projection.VSoldeCongeEmployeProjection;
import com.si.rh.repository.DemandeCongeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.TypeCongeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SoldeCongeService {

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private TypeCongeRepository typeCongeRepository;

    @Autowired
    private DemandeCongeRepository demandeCongeRepository;

    public List<SoldeCongeEmployeDTO> getSoldes() {
        List<VSoldeCongeEmployeProjection> projections = demandeCongeRepository.getSoldeConge();

        if (projections.isEmpty()) {
            return List.of();
        }

        List<Integer> idEmployes = projections.stream().map(VSoldeCongeEmployeProjection::getIdEmploye).distinct().toList();
        List<Integer> idTypes = projections.stream().map(VSoldeCongeEmployeProjection::getIdTypeConge).distinct().toList();

        Map<Integer, Employe> mapEmployes = employeRepository.findAllById(idEmployes)
                .stream().collect(Collectors.toMap(Employe::getId, e -> e));

        Map<Integer, TypeConge> mapTypes = typeCongeRepository.findAllById(idTypes)
                .stream().collect(Collectors.toMap(TypeConge::getId, tc -> tc));

        return projections.stream().map(scep -> {
            Employe e = mapEmployes.get(scep.getIdEmploye());
            TypeConge tc = mapTypes.get(scep.getIdTypeConge());

            if (e == null) {
                throw new RuntimeException("Aucun employé trouvé pour l'ID : " + scep.getIdEmploye());
            }
            if (tc == null) {
                throw new RuntimeException("Aucun type de congé trouvé pour l'ID : " + scep.getIdTypeConge());
            }

            return new SoldeCongeEmployeDTO(e.getId(), e.getNom(), e.getPrenom(), e.getMatricule(), e.getEmail(), tc.getLibelle(), scep.getSoldeDisponible());
        }).toList();
    }

}

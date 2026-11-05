package com.si.rh.service;

import com.si.rh.dto.conge.DemandeCongeDTO;
import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.model.Employe;
import com.si.rh.model.TypeConge;
import com.si.rh.projection.VCongeDemandesProjection;
import com.si.rh.projection.VSoldeCongeEmployeProjection;
import com.si.rh.repository.DemandeCongeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.TypeCongeRepository;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DemandeCongeService {
    @Autowired
    private DemandeCongeRepository demandeCongeRepository;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private TypeCongeRepository typeCongeRepository;

    public List<DemandeCongeDTO> getDemandesConge(@Nullable Integer id){
        List<VCongeDemandesProjection> projections = id == null ?  demandeCongeRepository.getAllDemandesVue() : demandeCongeRepository.findAllDemandesByEmployeId(id);

        if(projections.isEmpty()){
            return List.of();
        }

        List<Integer> idEmployes = projections.stream().map(VCongeDemandesProjection::getIdEmploye).distinct().toList();
        List<Integer> idTypes = projections.stream().map(VCongeDemandesProjection::getIdTypeConge).distinct().toList();

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

            return new DemandeCongeDTO(scep.getIdDemande(), e.getId(), e.getNom(), e.getPrenom(), e.getMatricule(), e.getEmail(), scep.getDateDebut(), scep.getDateFin(), tc.getLibelle(), scep.getNombreJours(), scep.getMotif());
        }).toList();
    }
}

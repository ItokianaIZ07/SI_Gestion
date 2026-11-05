package com.si.rh.service;

import com.si.rh.dto.conge.DemandeCongeDTO;
import com.si.rh.dto.conge.DemandeCongeRequest;
import com.si.rh.dto.conge.SoldeCongeEmployeDTO;
import com.si.rh.model.*;
import com.si.rh.model.enums.StatutCongeGlobal;
import com.si.rh.projection.VCongeDemandesProjection;
import com.si.rh.projection.VSoldeCongeEmployeProjection;
import com.si.rh.repository.*;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DemandeCongeService {
    @Autowired
    private DemandeCongeRepository demandeCongeRepository;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private TypeCongeRepository typeCongeRepository;

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private ValidationCongeRepository validationCongeRepository;

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

    @Transactional
    public DemandeConge save(DemandeCongeRequest request) {

        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new IllegalArgumentException(
                    "La date de fin doit être supérieure à la date de début."
            );
        }

        if (request.getNombreJours() <= 0) {
            throw new IllegalArgumentException(
                    "Le nombre de jours doit être positif."
            );
        }

        Decision decision = decisionRepository.findByCode("EN_ATTENTE")
                .orElseThrow(() ->
                        new RuntimeException("Aucune décision ne correspond au code EN_ATTENTE")
                );

        TypeConge typeConge = typeCongeRepository.findById(request.getIdTypeConge())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Aucun type de congé ne correspond à l'id : "
                                        + request.getIdTypeConge()
                        )
                );

        Employe employe = employeRepository.findById(request.getIdEmploye())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Aucun employé ne correspond à l'id "
                                        + request.getIdEmploye()
                        )
                );

        SoldeCongeEmployeDTO soldeDTO = demandeCongeRepository.getSoldeByEmpAndTypeConge(
                        request.getIdEmploye(),
                        request.getIdTypeConge()
                ).orElseThrow(()->
                        new RuntimeException(
                                "Impossible de récupérer le solde de congé de cet employé."
                        )
                );

        if (request.getNombreJours() > soldeDTO.solde()) {
            String message =  "Solde insuffisant. Solde disponible : "
                    + soldeDTO.solde()
                    + " jours, jours demandés : "
                    + request.getNombreJours();
            System.out.println(message);
            throw new IllegalArgumentException(message);
        }

        DemandeConge demandeConge = new DemandeConge();

        demandeConge.setDateDemande(LocalDateTime.now());
        demandeConge.setTypeConge(typeConge);
        demandeConge.setEmploye(employe);
        demandeConge.setDateDebut(request.getDateDebut());
        demandeConge.setDateFin(request.getDateFin());
        demandeConge.setMotif(request.getMotif());
        demandeConge.setStatutGlobal(StatutCongeGlobal.EN_ATTENTE_MANAGER);
        demandeConge.setNombreJours(request.getNombreJours());

        DemandeConge savedDemande = demandeCongeRepository.save(demandeConge);

        ValidationConge validationManager = new ValidationConge();

        validationManager.setDemandeConge(savedDemande);
        validationManager.setDecision(decision);
        validationManager.setOrdre(1);

        validationCongeRepository.save(validationManager);

        ValidationConge validationRH = new ValidationConge();

        validationRH.setDemandeConge(savedDemande);
        validationRH.setDecision(decision);
        validationRH.setOrdre(2);

        validationCongeRepository.save(validationRH);

        return savedDemande;
    }
}

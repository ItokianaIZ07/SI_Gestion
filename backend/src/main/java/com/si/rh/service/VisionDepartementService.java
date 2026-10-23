package com.si.rh.service;

import com.si.rh.dto.VisionDepartementResponse;
import com.si.rh.dto.DepartementResumeResponse;
import com.si.rh.model.Departement;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.PosteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VisionDepartementService {

    private final DepartementRepository departementRepository;
    private final EmployeRepository employeRepository;
    private final PosteRepository posteRepository;
    private final BesoinRepository besoinRepository;

    public VisionDepartementService(
            DepartementRepository departementRepository,
            EmployeRepository employeRepository,
            PosteRepository posteRepository,
            BesoinRepository besoinRepository
    ) {
        this.departementRepository = departementRepository;
        this.employeRepository = employeRepository;
        this.posteRepository = posteRepository;
        this.besoinRepository = besoinRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartementResumeResponse> getTousLesDepartements() {
        return departementRepository.findAllByOrderByNomAsc()
                .stream()
                .map(departement -> new DepartementResumeResponse(
                        departement.getId(),
                        departement.getNom(),
                        departement.getEffectifMax(),
                        employeRepository.countByDepartement_IdAndStatut(
                                departement.getId(),
                                StatutEmploye.ACTIF
                        ),
                        posteRepository.countByDepartement_Id(departement.getId()),
                        besoinRepository.countByPoste_Departement_Id(departement.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public VisionDepartementResponse getDepartement(Integer departementId) {
        Departement departement = departementRepository.findById(departementId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Département introuvable"
                ));
        return creerVision(departement);
    }

    private VisionDepartementResponse creerVision(Departement departement) {
        var employes = employeRepository.findByDepartement_Id(departement.getId());
        var postes = posteRepository.findByDepartement_IdOrderByNomAsc(departement.getId());
        var demandes = besoinRepository.findByPoste_Departement_IdOrderByDateDemandeDesc(departement.getId());
        var demandesParStatut = Arrays.stream(StatutGlobalBesoin.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        statut -> demandes.stream()
                                .filter(demande -> demande.getStatutGlobal() == statut)
                                .count(),
                        (premier, second) -> premier,
                        LinkedHashMap::new
                ));

        return new VisionDepartementResponse(
                departement.getId(),
                departement.getNom(),
                departement.getEffectifMax(),
                compterEmployes(employes, StatutEmploye.ACTIF),
                compterEmployes(employes, StatutEmploye.SUSPENDU),
                compterEmployes(employes, StatutEmploye.PARTI),
                postes.stream().filter(poste -> Boolean.TRUE.equals(poste.getActif())).count(),
                postes.stream().filter(poste -> !Boolean.TRUE.equals(poste.getActif())).count(),
                demandesParStatut,
                demandes.stream()
                        .map(demande -> new VisionDepartementResponse.DemandeResume(
                                demande.getId(),
                                demande.getPoste().getNom(),
                                demande.getEffectif(),
                                demande.getStatutGlobal().name(),
                                demande.getDateDemande()
                        ))
                        .toList()
        );
    }

    private long compterEmployes(List<com.si.rh.model.Employe> employes, StatutEmploye statut) {
        return employes.stream().filter(employe -> employe.getStatut() == statut).count();
    }
}

package com.si.rh.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.si.rh.dto.DepartementResumeResponse;
import com.si.rh.dto.DepartementRequest;
import com.si.rh.dto.VisionDepartementResponse;
import com.si.rh.model.Departement;
import com.si.rh.model.GrilleSalaire;
import com.si.rh.model.Poste;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.GrilleSalaireRepository;
import com.si.rh.repository.PosteRepository;
import com.si.rh.repository.UtilisateurRepository;

import org.springframework.transaction.annotation.Transactional;

@Service 
public class DepartementService {

    private final DepartementRepository departementRepository;
    private final EmployeRepository employeRepository;
    private final PosteRepository posteRepository;
    private final GrilleSalaireRepository grilleSalaireRepository;
    private final BesoinRepository besoinRepository;
    private final UtilisateurRepository utilisateurRepository;

    public DepartementService(
            DepartementRepository departementRepository,
            EmployeRepository employeRepository,
            PosteRepository posteRepository,
            GrilleSalaireRepository grilleSalaireRepository,
            BesoinRepository besoinRepository,
            UtilisateurRepository utilisateurRepository
    ) {
        this.departementRepository = departementRepository;
        this.employeRepository = employeRepository;
        this.posteRepository = posteRepository;
        this.grilleSalaireRepository = grilleSalaireRepository;
        this.besoinRepository = besoinRepository;
        this.utilisateurRepository = utilisateurRepository;
    }
    public List<Departement> getAll() {
        return departementRepository.findAll();
    }

    @Transactional
    public Departement creer(DepartementRequest request) {
        request.postes().forEach(poste -> {
            var grille = poste.grilleSalaire();
            if (grille.valeurMax().compareTo(grille.valeurMin()) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Le salaire maximum doit être supérieur ou égal au minimum."
                );
            }
            if (grille.dateFinValidite() != null
                    && grille.dateFinValidite().isBefore(grille.dateDebutValidite())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La date de fin doit suivre la date de début."
                );
            }
        });

        var manager = utilisateurRepository.findById(request.managerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Manager introuvable"
                ));

        var departement = new Departement();
        departement.setNom(request.nom().trim());
        departement.setManager(manager);
        departement.setEffectifMax(request.effectifMax());
        Departement departementEnregistre = departementRepository.save(departement);

        request.postes().forEach(posteRequest -> {
            Poste poste = new Poste();
            poste.setDepartement(departementEnregistre);
            poste.setNom(posteRequest.nom().trim());
            Poste posteEnregistre = posteRepository.save(poste);

            GrilleSalaire grille = new GrilleSalaire();
            grille.setPoste(posteEnregistre);
            grille.setValeurMin(posteRequest.grilleSalaire().valeurMin());
            grille.setValeurMax(posteRequest.grilleSalaire().valeurMax());
            grille.setDateDebutValidite(posteRequest.grilleSalaire().dateDebutValidite());
            grille.setDateFinValidite(posteRequest.grilleSalaire().dateFinValidite());
            grilleSalaireRepository.save(grille);
        });

        return departementEnregistre;
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

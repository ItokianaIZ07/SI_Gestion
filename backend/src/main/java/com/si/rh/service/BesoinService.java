package com.si.rh.service;

import com.si.rh.dto.BesoinRequest;
import com.si.rh.model.Besoin;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.PosteRepository;
import com.si.rh.repository.TypeBesoinRepository;
import com.si.rh.repository.TypeContratRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BesoinService {

    private final BesoinRepository besoinRepository;
    private final TypeBesoinRepository typeBesoinRepository;
    private final PosteRepository posteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final TypeContratRepository typeContratRepository;

    public BesoinService(
            BesoinRepository besoinRepository,
            TypeBesoinRepository typeBesoinRepository,
            PosteRepository posteRepository,
            UtilisateurRepository utilisateurRepository,
            EmployeRepository employeRepository,
            TypeContratRepository typeContratRepository
    ) {
        this.besoinRepository = besoinRepository;
        this.typeBesoinRepository = typeBesoinRepository;
        this.posteRepository = posteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.typeContratRepository = typeContratRepository;
    }

    public List<Besoin> getAll() {
        return besoinRepository.findAll();
    }

    @Transactional
    public Besoin creer(BesoinRequest request) {
        var typeBesoin = typeBesoinRepository.findById(request.typeBesoinId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de besoin introuvable"));
        var poste = posteRepository.findById(request.posteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poste introuvable"));
        var demandeur = utilisateurRepository.findById(request.demandeurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demandeur introuvable"));

        if (!poste.getDepartement().getId().equals(request.departementId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le poste ne correspond pas au département choisi");
        }

        var besoin = new Besoin();
        besoin.setTypeBesoin(typeBesoin);
        besoin.setPoste(poste);
        besoin.setDemandeur(demandeur);
        besoin.setEffectif(request.effectif());
        besoin.setPriorite(request.priorite());
        besoin.setDateArriveeSouhaitee(request.dateArriveeSouhaitee());
        besoin.setLieuTravail(request.lieuTravail());
        besoin.setMotif(request.motif().trim());
        besoin.setExperienceRequise(request.experienceRequise());
        besoin.setNiveauEtude(request.niveauEtude());
        besoin.setCompetencesRequises(request.competencesRequises());
        besoin.setSalaireEstime(request.salaireEstime());
        besoin.setStatutGlobal(StatutGlobalBesoin.EN_ATTENTE_RH);

        if (request.employeRemplaceId() != null) {
            besoin.setEmployeRemplace(employeRepository.findById(request.employeRemplaceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé remplacé introuvable")));
        }

        if (request.typeContratSouhaiteId() != null) {
            besoin.setTypeContratSouhaite(typeContratRepository.findById(request.typeContratSouhaiteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de contrat introuvable")));
        }

        return besoinRepository.save(besoin);
    }
}

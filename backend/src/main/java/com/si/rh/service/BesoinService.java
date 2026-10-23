package com.si.rh.service;

import com.si.rh.dto.BesoinRequest;
import com.si.rh.dto.besoin.BesoinDTO;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Besoin;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.PosteRepository;
import com.si.rh.repository.TypeBesoinRepository;
import com.si.rh.repository.TypeContratRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BesoinService {

    private final BesoinRepository besoinRepository;
    private final TypeBesoinRepository typeBesoinRepository;
    private final PosteRepository posteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final TypeContratRepository typeContratRepository;
    private final NotificationService notificationService;

    public BesoinService(
            BesoinRepository besoinRepository,
            TypeBesoinRepository typeBesoinRepository,
            PosteRepository posteRepository,
            UtilisateurRepository utilisateurRepository,
            EmployeRepository employeRepository,
            TypeContratRepository typeContratRepository,
            NotificationService notificationService
    ) {
        this.besoinRepository = besoinRepository;
        this.typeBesoinRepository = typeBesoinRepository;
        this.posteRepository = posteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.typeContratRepository = typeContratRepository;
        this.notificationService = notificationService;
    }

    // ---------- Lecture ----------

    public List<Besoin> getAll() {
        return besoinRepository.findAll();
    }

    public Besoin getById(int id) {
        return besoinRepository.getReferenceById(id);
    }

    public List<BesoinDTO> tous() {
        return besoinRepository.findAll(Sort.by(Sort.Direction.DESC, "dateDemande"))
                .stream()
                .map(this::dto)
                .toList();
    }

    public BesoinDTO parId(Integer id) {
        return dto(besoinRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Besoin introuvable : " + id)));
    }

    private BesoinDTO dto(Besoin b) {
        return new BesoinDTO(b.getId(), b.getTypeBesoin().getLibelle(), b.getPoste().getNom(),
                b.getPoste().getDepartement().getNom(), b.getEffectif(), b.getPriorite(),
                b.getDateArriveeSouhaitee(), b.getLieuTravail(), b.getMotif(), b.getExperienceRequise(),
                b.getNiveauEtude(), b.getCompetencesRequises(), b.getSalaireEstime(), b.getStatutGlobal(),
                b.getDateDemande());
    }

    // ---------- Écriture ----------

    // Nécessaire : sans cette annotation, save() hériterait de la transaction en lecture seule de la classe
    @Transactional
    public Besoin save(Besoin besoin) {
        return besoinRepository.save(besoin);
    }

    @Transactional
    public Besoin creer(BesoinRequest request) {
        validerChampsObligatoires(request);

        var typeBesoin = typeBesoinRepository.findById(request.typeBesoinId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de besoin introuvable"));
        var poste = posteRepository.findById(request.posteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poste introuvable"));
        var demandeur = utilisateurRepository.findById(request.demandeurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demandeur introuvable"));

        if (!poste.getDepartement().getId().equals(request.departementId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le poste ne correspond pas au département choisi");
        }

        if (estTypeRemplacement(typeBesoin) && (request.employeRemplaceId() == null || request.employeRemplaceId() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un employé remplacé est requis pour un besoin de remplacement");
        }

        var besoin = new Besoin();
        besoin.setTypeBesoin(typeBesoin);
        besoin.setPoste(poste);
        besoin.setDemandeur(demandeur);
        besoin.setEffectif(request.effectif());
        besoin.setPriorite(request.priorite());
        besoin.setDateArriveeSouhaitee(request.dateArriveeSouhaitee());
        besoin.setLieuTravail(request.lieuTravail().trim());
        besoin.setMotif(request.motif().trim());
        besoin.setExperienceRequise(request.experienceRequise().trim());
        besoin.setNiveauEtude(request.niveauEtude().trim());
        besoin.setCompetencesRequises(request.competencesRequises().trim());
        besoin.setSalaireEstime(request.salaireEstime());
        besoin.setStatutGlobal(StatutGlobalBesoin.EN_ATTENTE_RH);

        if (request.employeRemplaceId() != null && request.employeRemplaceId() > 0) {
            besoin.setEmployeRemplace(employeRepository.findById(request.employeRemplaceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé remplacé introuvable")));
        }

        besoin.setTypeContratSouhaite(typeContratRepository.findById(request.typeContratSouhaiteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de contrat introuvable")));

        Besoin besoinEnregistre = besoinRepository.save(besoin);
        notificationService.notifierNouveauBesoin(besoinEnregistre);
        return besoinEnregistre;
    }

    // ---------- Validation ----------

    private void validerChampsObligatoires(BesoinRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La demande est obligatoire");
        }

        exiger(request.demandeurId() != null && request.demandeurId() > 0, "demandeurId");
        exiger(request.departementId() != null && request.departementId() > 0, "departementId");
        exiger(request.typeBesoinId() != null && request.typeBesoinId() > 0, "typeBesoinId");
        exiger(request.posteId() != null && request.posteId() > 0, "posteId");
        exiger(request.typeContratSouhaiteId() != null && request.typeContratSouhaiteId() > 0, "typeContratSouhaiteId");
        exiger(request.effectif() != null && request.effectif() >= 1, "effectif");
        exiger(request.priorite() != null, "priorite");
        exiger(request.dateArriveeSouhaitee() != null
                && !request.dateArriveeSouhaitee().isBefore(LocalDate.now()), "dateArriveeSouhaitee");
        exiger(texteRenseigne(request.lieuTravail()), "lieuTravail");
        exiger(texteRenseigne(request.motif()), "motif");
        exiger(texteRenseigne(request.experienceRequise()), "experienceRequise");
        exiger(texteRenseigne(request.niveauEtude()), "niveauEtude");
        exiger(texteRenseigne(request.competencesRequises()), "competencesRequises");
        exiger(request.salaireEstime() != null
                && request.salaireEstime().compareTo(BigDecimal.ONE) > 0, "salaireEstime");

        var typeBesoin = typeBesoinRepository.findById(request.typeBesoinId());
        if (typeBesoin.isPresent() && estTypeRemplacement(typeBesoin.get())
                && (request.employeRemplaceId() == null || request.employeRemplaceId() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un employé remplacé est requis pour un besoin de remplacement");
        }
    }

    private boolean estTypeRemplacement(com.si.rh.model.TypeBesoin typeBesoin) {
        if (typeBesoin == null) {
            return false;
        }

        String code = typeBesoin.getCode() == null ? "" : typeBesoin.getCode().trim().toUpperCase();
        String libelle = typeBesoin.getLibelle() == null ? "" : typeBesoin.getLibelle().trim().toUpperCase();
        return "REMPLACEMENT".equals(code) || libelle.contains("REMPLACEMENT");
    }

    private boolean texteRenseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private void exiger(boolean condition, String champ) {
        if (!condition) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le champ " + champ + " est obligatoire et doit être valide"
            );
        }
    }
}
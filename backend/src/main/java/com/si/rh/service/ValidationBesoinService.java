package com.si.rh.service;

import com.si.rh.dto.BesoinValidationDTO;
import com.si.rh.model.*;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.ValidationBesoinRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationBesoinService {
    private static final List<String> ETAPES = List.of("RH", "FINANCE", "DIRECTION");
    private static final Set<String> ROLES_AUTORISES = Set.copyOf(ETAPES);

    private final ValidationBesoinRepository validationBesoinRepository;
    private final BesoinRepository besoinRepository;
    private final DecisionService decisionService;
    private final EtatValidationService etatValidationService;
    private final UtilisateurDetailsService utilisateurDetailsService;
    private final EmployeRepository employeRepository;

    public ValidationBesoinService(
            ValidationBesoinRepository validationBesoinRepository,
            BesoinRepository besoinRepository,
            DecisionService decisionService,
            EtatValidationService etatValidationService,
            UtilisateurDetailsService utilisateurDetailsService,
            DepartementRepository departementRepository,
            EmployeRepository employeRepository) {
        this.validationBesoinRepository = validationBesoinRepository;
        this.besoinRepository = besoinRepository;
        this.decisionService = decisionService;
        this.etatValidationService = etatValidationService;
        this.utilisateurDetailsService = utilisateurDetailsService;
        this.employeRepository = employeRepository;
    }

    @Transactional(readOnly = true)
    public List<BesoinValidationDTO> getBesoinsAvecEtat(String emailUtilisateur) {
        Utilisateur utilisateur = utilisateurDetailsService.getByEmail(emailUtilisateur);
        List<Besoin> besoins = besoinRepository.findAll();
        if (besoins.isEmpty()) {
            return List.of();
        }

        Collection<Integer> ids = besoins.stream().map(Besoin::getId).toList();
        Map<Integer, List<ValidationBesoin>> validationsParBesoin =
                validationBesoinRepository.findAllForBesoins(ids).stream()
                        .collect(Collectors.groupingBy(v -> v.getBesoin().getId()));

        return besoins.stream()
                .map(besoin -> toDto(
                        besoin,
                        validationsParBesoin.getOrDefault(besoin.getId(), List.of()),
                        utilisateur.getRole().getCode()))
                .toList();
    }

    @Transactional
    public ValidationBesoin prendreDecision(
            Integer idBesoin,
            String codeDecision,
            String remarque,
            String emailUtilisateur) {
        Utilisateur utilisateur = utilisateurDetailsService.getByEmail(emailUtilisateur);
        String role = normaliserCode(utilisateur.getRole().getCode());
        if (!isRoleAutorise(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre rôle ne peut pas valider un besoin.");
        }

        String decisionCode = normaliserCode(codeDecision);
        if (!"ACCEPTE".equals(decisionCode) && !"REFUSE".equals(decisionCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La décision doit être ACCEPTE ou REFUSE.");
        }

        Besoin besoin = besoinRepository.findByIdForUpdate(idBesoin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Besoin introuvable."));
        List<ValidationBesoin> historique = new ArrayList<>(
                validationBesoinRepository.findAllForBesoins(List.of(idBesoin)));
        Map<String, ValidationBesoin> validationsCourantes = decisionsCourantes(historique);

        if (!peutDecider(role, validationsCourantes)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette étape ne peut pas décider : le besoin est terminé, l'étape a déjà répondu ou le RH n'a pas accepté.");
        }

        Decision decision = decisionService.getByCode(decisionCode);
        EtatValidation etat = etatValidationService.getByCode(role);

        ValidationBesoin nouvelleValidation = new ValidationBesoin();
        nouvelleValidation.setBesoin(besoin);
        nouvelleValidation.setValidateur(utilisateur);
        nouvelleValidation.setEtatValidation(etat);
        nouvelleValidation.setDecision(decision);
        nouvelleValidation.setDateDecision(OffsetDateTime.now());
        nouvelleValidation.setRemarque(remarque);

        ValidationBesoin sauvegardee = validationBesoinRepository.saveAndFlush(nouvelleValidation);
        historique.add(sauvegardee);
        synchroniserStatutEnBase(besoin, decisionsCourantes(historique));
        return sauvegardee;
    }

    @Transactional(readOnly = true)
    public boolean isRoleAutorise(String role) {
        return role != null && ROLES_AUTORISES.contains(normaliserCode(role));
    }

    @Transactional(readOnly = true)
    public boolean rhAccepte(Integer idBesoin) {
        return "ACCEPTE".equals(decisionCourante(idBesoin, "RH"));
    }

    @Transactional(readOnly = true)
    public boolean etapeEnAttente(Integer idBesoin, String role) {
        return "EN_ATTENTE".equals(decisionCourante(idBesoin, role));
    }

    @Transactional(readOnly = true)
    public boolean besoinRefuse(Integer idBesoin) {
        return getStatutGlobal(idBesoin).equals("REFUSE");
    }

    @Transactional(readOnly = true)
    public boolean toutesEtapesAcceptees(Integer idBesoin) {
        return getStatutGlobal(idBesoin).equals("ACCEPTE");
    }

    @Transactional(readOnly = true)
    public String getStatutGlobal(Integer idBesoin) {
        return determinerStatutGlobal(decisionsCourantes(
                validationBesoinRepository.findAllForBesoins(List.of(idBesoin))));
    }

    @Transactional(readOnly = true)
    public boolean peutPrendreDecision(Integer idBesoin, String role) {
        return peutDecider(role, decisionsCourantes(
                validationBesoinRepository.findAllForBesoins(List.of(idBesoin))));
    }

    public String determinerStatutGlobal(Map<String, ValidationBesoin> validationsCourantes) {
        if (aEteRefuse(validationsCourantes)) {
            return "REFUSE";
        }
        if (ETAPES.stream().allMatch(etape -> "ACCEPTE".equals(decision(validationsCourantes, etape)))) {
            return "ACCEPTE";
        }
        return "EN_ATTENTE";
    }

    private BesoinValidationDTO toDto(
            Besoin besoin,
            List<ValidationBesoin> historique,
            String role) {
        Map<String, ValidationBesoin> courantes = decisionsCourantes(historique);
        boolean peutDecider = peutDecider(role, courantes);
        Departement departement = besoin.getPoste().getDepartement();
        return new BesoinValidationDTO(
                besoin.getId(),
                besoin.getPoste().getNom(),
                besoin.getPoste().getDepartement().getNom(),
                besoin.getDemandeur().getPrenom() + " " + besoin.getDemandeur().getNom(),
                besoin.getTypeBesoin().getLibelle(),
                besoin.getEffectif(),
                (int) employeRepository.countByDepartement_IdAndStatut(departement.getId(), StatutEmploye.ACTIF),
                departement.getEffectifMax(),
                besoin.getPriorite().name(),
                besoin.getLieuTravail(),
                besoin.getMotif(),
                besoin.getExperienceRequise(),
                besoin.getNiveauEtude(),
                besoin.getCompetencesRequises(),
                besoin.getSalaireEstime(),
                besoin.getDateArriveeSouhaitee(),
                besoin.getDateDemande(),
                determinerStatutGlobal(courantes),
                decision(courantes, "RH"),
                decision(courantes, "FINANCE"),
                decision(courantes, "DIRECTION"),
                peutDecider,
                !peutDecider,
                !peutDecider);
    }

    private boolean peutDecider(String role, Map<String, ValidationBesoin> validationsCourantes) {
        String roleNormalise = normaliserCode(role);
        if (!isRoleAutorise(roleNormalise)
                || aEteRefuse(validationsCourantes)
                || !"EN_ATTENTE".equals(decision(validationsCourantes, roleNormalise))) {
            return false;
        }
        return "RH".equals(roleNormalise) || "ACCEPTE".equals(decision(validationsCourantes, "RH"));
    }

    private boolean aEteRefuse(Map<String, ValidationBesoin> validationsCourantes) {
        return ETAPES.stream().anyMatch(etape -> "REFUSE".equals(decision(validationsCourantes, etape)));
    }

    private String decisionCourante(Integer idBesoin, String role) {
        return decision(
                decisionsCourantes(validationBesoinRepository.findAllForBesoins(List.of(idBesoin))),
                normaliserCode(role));
    }

    private Map<String, ValidationBesoin> decisionsCourantes(List<ValidationBesoin> historique) {
        Comparator<ValidationBesoin> ordreRecence = Comparator
                .comparing(
                        ValidationBesoin::getDateDecision,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ValidationBesoin::getId, Comparator.nullsFirst(Comparator.naturalOrder()));

        Map<String, ValidationBesoin> courantes = new HashMap<>();
        for (ValidationBesoin validation : historique) {
            String etape = validation.getEtatValidation().getCode();
//            map.merge(cle, valeurInitiale, (ancienneValeur, nouvelleValeur) -> fonctionDeFusion);
            courantes.merge(etape, validation,
                    (existante, candidate) -> ordreRecence.compare(candidate, existante) > 0
                            ? candidate : existante);
        }
        return courantes;
    }

    private String decision(Map<String, ValidationBesoin> validationsCourantes, String role) {
        ValidationBesoin validation = validationsCourantes.get(role);
        return validation == null ? "EN_ATTENTE" : validation.getDecision().getCode();
    }

    private void synchroniserStatutEnBase(
            Besoin besoin,
            Map<String, ValidationBesoin> validationsCourantes) {
        String statut = determinerStatutGlobal(validationsCourantes);
        if ("REFUSE".equals(statut)) {
            besoin.setStatutGlobal(StatutGlobalBesoin.REFUSE);
        } else if ("ACCEPTE".equals(statut)) {
            besoin.setStatutGlobal(StatutGlobalBesoin.ACCEPTE);
        } else if (!"ACCEPTE".equals(decision(validationsCourantes, "RH"))) {
            besoin.setStatutGlobal(StatutGlobalBesoin.EN_ATTENTE_RH);
        } else if (!"ACCEPTE".equals(decision(validationsCourantes, "FINANCE"))) {
            besoin.setStatutGlobal(StatutGlobalBesoin.EN_ATTENTE_FINANCE);
        } else {
            besoin.setStatutGlobal(StatutGlobalBesoin.EN_ATTENTE_DIRECTION);
        }
        besoinRepository.save(besoin);
    }

    private String normaliserCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}

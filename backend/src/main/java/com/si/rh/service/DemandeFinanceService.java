package com.si.rh.service;

// BY : Fenosoa
// Demandes de recrutement vues par la Finance + validation / refus.
//
// Workflow séquentiel : RH -> FINANCE -> DIRECTION.
//   - Les boutons ne sont actifs que si la demande est « EN_ATTENTE_FINANCE » (RH a accepté)
//     et que la Finance n'a pas encore décidé.
//   - « Valider » exige en plus un salaire estimé et un budget restant suffisant.
//   - Un refus de la Finance refuse automatiquement l'étape Direction (comme dans les données de test).
// Tout est revérifié ici : le front n'est qu'un affichage.

import com.si.rh.dto.BudgetResponse.LigneDepartement;
import com.si.rh.dto.DemandeFinanceResponse;
import com.si.rh.dto.DemandeFinanceResponse.Etapes;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.Besoin;
import com.si.rh.model.Decision;
import com.si.rh.model.EtatValidation;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.ValidationBesoin;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.DecisionRepository;
import com.si.rh.repository.EtatValidationRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.repository.ValidationBesoinRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemandeFinanceService {

    /** Coût d'une demande = effectif x salaire mensuel estimé x 12 mois (coût annuel, par prudence). */
    static final int NB_MOIS = 12;

    private static final String RH = "RH";
    private static final String FINANCE = "FINANCE";
    private static final String DIRECTION = "DIRECTION";

    private static final String EN_ATTENTE = "EN_ATTENTE";
    private static final String ACCEPTE = "ACCEPTE";
    private static final String REFUSE = "REFUSE";

    private final BesoinRepository besoinRepository;
    private final ValidationBesoinRepository validationRepository;
    private final EtatValidationRepository etatRepository;
    private final DecisionRepository decisionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final BudgetService budgetService;

    public DemandeFinanceService(BesoinRepository besoinRepository,
                                 ValidationBesoinRepository validationRepository,
                                 EtatValidationRepository etatRepository,
                                 DecisionRepository decisionRepository,
                                 UtilisateurRepository utilisateurRepository,
                                 BudgetService budgetService) {
        this.besoinRepository = besoinRepository;
        this.validationRepository = validationRepository;
        this.etatRepository = etatRepository;
        this.decisionRepository = decisionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.budgetService = budgetService;
    }

    // =====================================================================
    // Lecture
    // =====================================================================

    @Transactional(readOnly = true)
    public List<DemandeFinanceResponse> lister() {
        List<Besoin> besoins = besoinRepository.findPourFinance();
        if (besoins.isEmpty()) {
            return List.of();
        }

        // Décisions de chaque besoin : { idBesoin -> { étape -> décision } }
        Map<Integer, Map<String, String>> decisions = new HashMap<>();
        List<Integer> ids = besoins.stream().map(Besoin::getId).toList();
        for (ValidationBesoin v : validationRepository.findByBesoinIds(ids)) {
            decisions.computeIfAbsent(v.getBesoin().getId(), k -> new HashMap<>())
                    .put(v.getEtatValidation().getCode(), v.getDecision().getCode());
        }

        // Budget calculé une seule fois par année (et non une fois par demande).
        Map<Integer, Map<Integer, LigneDepartement>> budgetsParAnnee = new HashMap<>();

        return besoins.stream().map(b -> {
            int annee = anneeBudget(b);
            Map<Integer, LigneDepartement> budgets = budgetsParAnnee.computeIfAbsent(annee, this::budgetParDepartement);
            Evaluation ev = evaluer(b, decisions.getOrDefault(b.getId(), Map.of()),
                    budgets.get(b.getPoste().getDepartement().getId()));

            return new DemandeFinanceResponse(
                    b.getId(),
                    b.getPoste().getNom(),
                    b.getPoste().getDepartement().getNom(),
                    b.getEffectif(),
                    b.getSalaireEstime(),
                    NB_MOIS,
                    ev.coutTotal(),
                    ev.budgetRestant(),
                    ev.couvert(),
                    ev.etapes(),
                    b.getStatutGlobal().name(),
                    ev.peutValider(),
                    ev.peutRefuser(),
                    ev.motifBlocage());
        }).toList();
    }

    // =====================================================================
    // Actions
    // =====================================================================

    @Transactional
    public void valider(Integer idBesoin, String emailValidateur) {
        Besoin besoin = chargerPourMiseAJour(idBesoin);
        Utilisateur validateur = chargerValidateur(emailValidateur);
        Map<String, ValidationBesoin> lignes = lignesDuBesoin(besoin);

        Evaluation ev = evaluerBesoin(besoin, lignes);
        if (!ev.peutValider()) {
            throw new RegleMetierException(HttpStatus.CONFLICT, ev.motifBlocage());
        }

        ecrire(besoin, lignes, FINANCE, ACCEPTE, validateur, null);
        if (!lignes.containsKey(DIRECTION)) {
            ecrire(besoin, lignes, DIRECTION, EN_ATTENTE, null, null);   // prochaine étape
        }
        besoin.setStatutGlobal(StatutGlobalBesoin.EN_ATTENTE_DIRECTION);
        besoinRepository.save(besoin);
    }

    @Transactional
    public void refuser(Integer idBesoin, String emailValidateur, String remarque) {
        Besoin besoin = chargerPourMiseAJour(idBesoin);
        Utilisateur validateur = chargerValidateur(emailValidateur);
        Map<String, ValidationBesoin> lignes = lignesDuBesoin(besoin);

        Evaluation ev = evaluerBesoin(besoin, lignes);
        if (!ev.peutRefuser()) {
            throw new RegleMetierException(HttpStatus.CONFLICT, ev.motifBlocage());
        }

        ecrire(besoin, lignes, FINANCE, REFUSE, validateur, remarque);
        ecrire(besoin, lignes, DIRECTION, REFUSE, validateur, "Refus automatique suite au refus de Finance");
        besoin.setStatutGlobal(StatutGlobalBesoin.REFUSE);
        besoinRepository.save(besoin);
    }

    // =====================================================================
    // Règles métier
    // =====================================================================

    /** Tout ce que le tableau affiche pour une demande, calculé en un seul endroit. */
    private record Evaluation(
            Etapes etapes,
            BigDecimal coutTotal,
            BigDecimal budgetRestant,
            boolean couvert,
            boolean peutValider,
            boolean peutRefuser,
            String motifBlocage) {}

    private Evaluation evaluerBesoin(Besoin b, Map<String, ValidationBesoin> lignes) {
        Map<String, String> decisions = new HashMap<>();
        lignes.forEach((etape, v) -> decisions.put(etape, v.getDecision().getCode()));

        LigneDepartement budget = budgetParDepartement(anneeBudget(b)).get(b.getPoste().getDepartement().getId());
        return evaluer(b, decisions, budget);
    }

    private Evaluation evaluer(Besoin b, Map<String, String> decisions, LigneDepartement budget) {
        StatutGlobalBesoin statut = b.getStatutGlobal();
        Etapes etapes = new Etapes(
                decisionEtape(RH, decisions, statut),
                decisionEtape(FINANCE, decisions, statut),
                decisionEtape(DIRECTION, decisions, statut));

        BigDecimal salaire = b.getSalaireEstime();
        BigDecimal cout = salaire == null ? null
                : salaire.multiply(BigDecimal.valueOf(b.getEffectif())).multiply(BigDecimal.valueOf(NB_MOIS));
        BigDecimal restant = budget == null ? BigDecimal.ZERO : budget.restant();
        boolean couvert = cout != null && cout.compareTo(restant) <= 0;

        // 1) Le workflow autorise-t-il la Finance à agir ?
        String blocage = motifWorkflow(statut, etapes);
        boolean peutRefuser = blocage == null;

        // 2) « Valider » demande en plus un salaire estimé et un budget suffisant.
        boolean peutValider = peutRefuser && salaire != null && couvert;
        if (peutRefuser && !peutValider) {
            blocage = salaire == null
                    ? "Le salaire estimé n'a pas encore été défini par les RH."
                    : "Le coût total dépasse le budget restant du département.";
        }

        return new Evaluation(etapes, cout, restant, couvert, peutValider, peutRefuser, blocage);
    }

    /** Raison pour laquelle la Finance ne peut pas agir, ou null si le workflow l'autorise. */
    private String motifWorkflow(StatutGlobalBesoin statut, Etapes etapes) {
        if (REFUSE.equals(etapes.finance())) {
            return "Déjà refusée par la Finance.";
        }
        if (ACCEPTE.equals(etapes.finance())) {
            return "Déjà validée par la Finance.";
        }
        return switch (statut) {
            case EN_ATTENTE_FINANCE -> null;
            case EN_ATTENTE_RH -> REFUSE.equals(etapes.rh())
                    ? "Refusée par les RH."
                    : "En attente de la validation des RH.";
            case REFUSE -> REFUSE.equals(etapes.rh()) ? "Refusée par les RH." : "Demande refusée.";
            case EN_ATTENTE_DIRECTION, ACCEPTE -> "Demande déjà traitée par la Finance.";
            case ANNULE -> "Demande annulée.";
        };
    }

    /**
     * Décision d'une étape. Si la ligne n'existe pas dans validation_besoin, on la déduit du
     * statut global (utile si un autre module ne crée pas les 3 lignes d'un coup).
     */
    private String decisionEtape(String etape, Map<String, String> decisions, StatutGlobalBesoin statut) {
        String enBase = decisions.get(etape);
        if (enBase != null) {
            return enBase;
        }
        boolean rhFait = statut == StatutGlobalBesoin.EN_ATTENTE_FINANCE
                || statut == StatutGlobalBesoin.EN_ATTENTE_DIRECTION
                || statut == StatutGlobalBesoin.ACCEPTE;
        boolean financeFait = statut == StatutGlobalBesoin.EN_ATTENTE_DIRECTION
                || statut == StatutGlobalBesoin.ACCEPTE;
        boolean directionFait = statut == StatutGlobalBesoin.ACCEPTE;
        boolean fait = switch (etape) {
            case RH -> rhFait;
            case FINANCE -> financeFait;
            default -> directionFait;
        };
        return fait ? ACCEPTE : EN_ATTENTE;
    }

    /** Le budget d'une demande est celui de l'année d'arrivée souhaitée (année en cours par défaut). */
    private int anneeBudget(Besoin b) {
        return b.getDateArriveeSouhaitee() != null
                ? b.getDateArriveeSouhaitee().getYear()
                : LocalDate.now().getYear();
    }

    private Map<Integer, LigneDepartement> budgetParDepartement(int annee) {
        Map<Integer, LigneDepartement> map = new HashMap<>();
        budgetService.calculer(annee).departements().forEach(l -> map.put(l.idDepartement(), l));
        return map;
    }

    // =====================================================================
    // Accès base
    // =====================================================================

    private Besoin chargerPourMiseAJour(Integer idBesoin) {
        return besoinRepository.findByIdPourMiseAJour(idBesoin)
                .orElseThrow(() -> new RegleMetierException(HttpStatus.NOT_FOUND, "Demande introuvable."));
    }

    private Utilisateur chargerValidateur(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RegleMetierException(HttpStatus.UNAUTHORIZED, "Utilisateur non reconnu."));
    }

    private Map<String, ValidationBesoin> lignesDuBesoin(Besoin besoin) {
        Map<String, ValidationBesoin> lignes = new HashMap<>();
        for (ValidationBesoin v : validationRepository.findByBesoinIds(List.of(besoin.getId()))) {
            lignes.put(v.getEtatValidation().getCode(), v);
        }
        return lignes;
    }

    /**
     * Écrit la décision d'une étape (crée la ligne si elle n'existe pas encore).
     * Le trigger SQL exige un validateur et une date pour ACCEPTE/REFUSE, et aucun des deux pour EN_ATTENTE.
     */
    private void ecrire(Besoin besoin, Map<String, ValidationBesoin> lignes, String etape,
                        String codeDecision, Utilisateur validateur, String remarque) {
        ValidationBesoin v = lignes.get(etape);
        boolean nouvelle = v == null;
        if (nouvelle) {
            v = new ValidationBesoin();
            v.setBesoin(besoin);
            v.setEtatValidation(etatParCode(etape));
        }

        boolean enAttente = EN_ATTENTE.equals(codeDecision);
        v.setDecision(decisionParCode(codeDecision));
        v.setValidateur(enAttente ? null : validateur);
        v.setDateDecision(enAttente ? null : OffsetDateTime.now());
        v.setRemarque(remarque);

        if (nouvelle) {
            validationRepository.save(v);   // insertion immédiate (id IDENTITY) : tous les champs sont déjà remplis
            lignes.put(etape, v);
        }
    }

    private EtatValidation etatParCode(String code) {
        return etatRepository.findByCode(code).orElseThrow(() ->
                new RegleMetierException(HttpStatus.INTERNAL_SERVER_ERROR, "Étape de validation inconnue : " + code));
    }

    private Decision decisionParCode(String code) {
        return decisionRepository.findByCode(code).orElseThrow(() ->
                new RegleMetierException(HttpStatus.INTERNAL_SERVER_ERROR, "Décision inconnue : " + code));
    }
}
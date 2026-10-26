package com.si.rh.service;

// BY : Fenosoa
// Demandes de recrutement vues par la Finance : liste, détail, validation et refus.
//
// Workflow séquentiel : RH -> FINANCE -> DIRECTION. Un refus à n'importe quelle étape termine la demande.
//   - La Finance décide seulement quand les RH ont accepté (statut EN_ATTENTE_FINANCE).
//   - Seul le rôle FINANCE peut valider ou refuser (la Direction consulte, mais ne décide pas à sa place).
//   - « Valider » exige : un salaire estimé par les RH, un salaire dans la grille en vigueur du poste,
//     et un budget restant suffisant. « Refuser » reste possible (remarque obligatoire).
//   - Un refus de la Finance refuse automatiquement l'étape Direction.
// Chaque décision AJOUTE une ligne dans validation_besoin (historique) ; la décision courante d'une étape
// est la plus récente — même règle que ValidationBesoinService.

import com.si.rh.dto.BudgetResponse.LigneDepartement;
import com.si.rh.dto.DemandeFinanceDetailResponse;
import com.si.rh.dto.DemandeFinanceDetailResponse.Budget;
import com.si.rh.dto.DemandeFinanceDetailResponse.Etape;
import com.si.rh.dto.DemandeFinanceDetailResponse.Grille;
import com.si.rh.dto.DemandeFinanceResponse;
import com.si.rh.dto.DemandeFinanceResponse.Etapes;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.Besoin;
import com.si.rh.model.Decision;
import com.si.rh.model.EtatValidation;
import com.si.rh.model.GrilleSalaire;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.ValidationBesoin;
import com.si.rh.model.enums.StatutGlobalBesoin;
import com.si.rh.repository.BesoinRepository;
import com.si.rh.repository.DecisionRepository;
import com.si.rh.repository.EtatValidationRepository;
import com.si.rh.repository.GrilleSalaireRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.repository.ValidationBesoinRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private static final String BLOQUE = "BLOQUE";   // étape pas encore atteinte (affichage seulement)

    private static final Comparator<ValidationBesoin> ORDRE_RECENCE = Comparator
            .comparing(ValidationBesoin::getDateDecision, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(ValidationBesoin::getId, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final BesoinRepository besoinRepository;
    private final ValidationBesoinRepository validationRepository;
    private final EtatValidationRepository etatRepository;
    private final DecisionRepository decisionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final GrilleSalaireRepository grilleRepository;
    private final BudgetService budgetService;

    public DemandeFinanceService(BesoinRepository besoinRepository,
                                 ValidationBesoinRepository validationRepository,
                                 EtatValidationRepository etatRepository,
                                 DecisionRepository decisionRepository,
                                 UtilisateurRepository utilisateurRepository,
                                 GrilleSalaireRepository grilleRepository,
                                 BudgetService budgetService) {
        this.besoinRepository = besoinRepository;
        this.validationRepository = validationRepository;
        this.etatRepository = etatRepository;
        this.decisionRepository = decisionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.grilleRepository = grilleRepository;
        this.budgetService = budgetService;
    }

    // =====================================================================
    // Lecture
    // =====================================================================

    /** Liste légère : une ligne par demande, sans les boutons (la décision se prend dans le détail). */
    @Transactional(readOnly = true)
    public List<DemandeFinanceResponse> lister() {
        List<Besoin> besoins = besoinRepository.findPourFinance();
        if (besoins.isEmpty()) {
            return List.of();
        }

        Map<Integer, Map<String, ValidationBesoin>> courantes =
                validationsCourantes(besoins.stream().map(Besoin::getId).toList());

        // Budget calculé une seule fois par année (et non une fois par demande).
        Map<Integer, Map<Integer, LigneDepartement>> budgetsParAnnee = new HashMap<>();

        return besoins.stream().map(b -> {
            Etapes etapes = etapesDe(b, courantes.getOrDefault(b.getId(), Map.of()));
            LigneDepartement budget = budgetsParAnnee
                    .computeIfAbsent(anneeBudget(b), this::budgetParDepartement)
                    .get(b.getPoste().getDepartement().getId());

            BigDecimal cout = coutTotal(b);
            boolean couvert = cout != null && cout.compareTo(restantDe(budget)) <= 0;

            return new DemandeFinanceResponse(
                    b.getId(),
                    b.getPoste().getNom(),
                    b.getPoste().getDepartement().getNom(),
                    b.getEffectif(),
                    cout,
                    couvert,
                    etapes,
                    b.getStatutGlobal().name());
        }).toList();
    }

    /** Détail complet d'une demande, avec les droits de l'utilisateur connecté. */
    @Transactional(readOnly = true)
    public DemandeFinanceDetailResponse detail(Integer idBesoin, String emailUtilisateur) {
        Besoin b = besoinRepository.findById(idBesoin)
                .orElseThrow(() -> new RegleMetierException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        Utilisateur utilisateur = chargerUtilisateur(emailUtilisateur);
        Map<String, ValidationBesoin> courantes = courantesDuBesoin(idBesoin);

        LigneDepartement budget = budgetDe(b);
        GrilleSalaire grille = grilleDe(b);
        Evaluation ev = evaluer(b, courantes, budget, grille, estFinance(utilisateur));

        BigDecimal restant = restantDe(budget);
        BigDecimal restantApres = ev.coutTotal() == null ? restant : restant.subtract(ev.coutTotal());

        List<Etape> etapes = List.of(
                etapeDetail(RH, "Ressources humaines", ev.etapes(), courantes),
                etapeDetail(FINANCE, "Finance", ev.etapes(), courantes),
                etapeDetail(DIRECTION, "Direction", ev.etapes(), courantes));

        return new DemandeFinanceDetailResponse(
                b.getId(),
                b.getPoste().getNom(),
                b.getPoste().getDepartement().getNom(),
                b.getStatutGlobal().name(),
                b.getPriorite().name(),

                b.getDemandeur().getPrenom() + " " + b.getDemandeur().getNom(),
                b.getTypeBesoin().getLibelle(),
                b.getEffectif(),
                b.getTypeContratSouhaite() != null ? b.getTypeContratSouhaite().getLibelle() : null,
                b.getLieuTravail(),
                b.getDateDemande(),
                b.getDateArriveeSouhaitee(),

                b.getMotif(),
                b.getExperienceRequise(),
                b.getNiveauEtude(),
                b.getCompetencesRequises(),

                b.getSalaireEstime(),
                NB_MOIS,
                ev.coutTotal(),

                new Budget(anneeBudget(b),
                        budget == null ? BigDecimal.ZERO : budget.budget(),
                        budget == null ? BigDecimal.ZERO : budget.masseSalariale(),
                        budget == null ? BigDecimal.ZERO : budget.depenses(),
                        restant, restantApres, ev.couvert()),
                new Grille(grille != null,
                        grille == null ? null : grille.getValeurMin(),
                        grille == null ? null : grille.getValeurMax(),
                        ev.dansGrille()),
                etapes,

                ev.peutValider(),
                ev.peutRefuser(),
                ev.motifBlocage());
    }

    // =====================================================================
    // Actions (réservées au rôle FINANCE)
    // =====================================================================

    @Transactional
    public void valider(Integer idBesoin, String emailUtilisateur, String remarque) {
        Besoin besoin = chargerPourMiseAJour(idBesoin);
        Utilisateur utilisateur = chargerUtilisateur(emailUtilisateur);
        exigerFinance(utilisateur);

        Evaluation ev = evaluer(besoin, courantesDuBesoin(idBesoin), budgetDe(besoin), grilleDe(besoin), true);
        if (!ev.peutValider()) {
            throw new RegleMetierException(HttpStatus.CONFLICT, ev.motifBlocage());
        }

        enregistrer(besoin, FINANCE, ACCEPTE, utilisateur, texteOuNull(remarque));
        besoin.setStatutGlobal(StatutGlobalBesoin.EN_ATTENTE_DIRECTION);
        besoinRepository.save(besoin);
    }

    @Transactional
    public void refuser(Integer idBesoin, String emailUtilisateur, String remarque) {
        if (texteOuNull(remarque) == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "La remarque est obligatoire pour refuser une demande.");
        }
        Besoin besoin = chargerPourMiseAJour(idBesoin);
        Utilisateur utilisateur = chargerUtilisateur(emailUtilisateur);
        exigerFinance(utilisateur);

        Evaluation ev = evaluer(besoin, courantesDuBesoin(idBesoin), budgetDe(besoin), grilleDe(besoin), true);
        if (!ev.peutRefuser()) {
            throw new RegleMetierException(HttpStatus.CONFLICT, ev.motifBlocage());
        }

        enregistrer(besoin, FINANCE, REFUSE, utilisateur, remarque.trim());
        enregistrer(besoin, DIRECTION, REFUSE, utilisateur, "Refus automatique suite au refus de Finance");
        besoin.setStatutGlobal(StatutGlobalBesoin.REFUSE);
        besoinRepository.save(besoin);
    }

    // =====================================================================
    // Règles métier
    // =====================================================================

    /** Tout ce qu'il faut savoir d'une demande pour la Finance, calculé en un seul endroit. */
    private record Evaluation(
            Etapes etapes,
            BigDecimal coutTotal,
            boolean couvert,
            boolean dansGrille,
            boolean peutValider,
            boolean peutRefuser,
            String motifBlocage) {}

    private Evaluation evaluer(Besoin b, Map<String, ValidationBesoin> courantes,
                               LigneDepartement budget, GrilleSalaire grille, boolean financeConnectee) {
        Etapes etapes = etapesDe(b, courantes);

        BigDecimal salaire = b.getSalaireEstime();
        BigDecimal cout = coutTotal(b);
        BigDecimal restant = restantDe(budget);
        boolean couvert = cout != null && cout.compareTo(restant) <= 0;
        boolean dansGrille = grille != null && salaire != null
                && salaire.compareTo(grille.getValeurMin()) >= 0
                && salaire.compareTo(grille.getValeurMax()) <= 0;

        // 1) Le workflow autorise-t-il la Finance à agir ? 2) L'utilisateur est-il de la Finance ?
        String blocage = motifWorkflow(b.getStatutGlobal(), etapes);
        if (blocage == null && !financeConnectee) {
            blocage = "Seule la Finance peut valider ou refuser cette demande.";
        }
        boolean peutRefuser = blocage == null;
        boolean peutValider = peutRefuser;

        // 3) « Valider » exige en plus salaire, grille et budget (« Refuser » reste possible).
        if (peutRefuser) {
            if (salaire == null) {
                blocage = "Le salaire estimé n'a pas encore été défini par les RH.";
            } else if (grille == null) {
                blocage = "Aucune grille salariale en vigueur pour ce poste.";
            } else if (!dansGrille) {
                blocage = "Salaire estimé hors de la grille (" + ariary(grille.getValeurMin())
                        + " à " + ariary(grille.getValeurMax()) + ").";
            } else if (!couvert) {
                blocage = "Budget insuffisant : coût " + ariary(cout) + ", restant " + ariary(restant) + ".";
            }
            peutValider = blocage == null;
        }

        return new Evaluation(etapes, cout, couvert, dansGrille, peutValider, peutRefuser, blocage);
    }

    /** Raison pour laquelle le workflow empêche la Finance d'agir, ou null s'il l'autorise. */
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

    private Etapes etapesDe(Besoin b, Map<String, ValidationBesoin> courantes) {
        Map<String, String> decisions = new HashMap<>();
        courantes.forEach((etape, v) -> decisions.put(etape, v.getDecision().getCode()));
        StatutGlobalBesoin statut = b.getStatutGlobal();
        return new Etapes(
                decisionEtape(RH, decisions, statut),
                decisionEtape(FINANCE, decisions, statut),
                decisionEtape(DIRECTION, decisions, statut));
    }

    /**
     * Décision d'une étape. Si aucune ligne n'existe dans validation_besoin, on la déduit du statut global
     * (utile si un autre module ne crée pas les lignes d'étapes).
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

    /** Une étape n'est « atteinte » que si la précédente est acceptée (workflow séquentiel). */
    private String etatAffiche(String etape, Etapes e) {
        return switch (etape) {
            case RH -> e.rh();
            case FINANCE -> ACCEPTE.equals(e.rh()) ? e.finance() : BLOQUE;
            default -> ACCEPTE.equals(e.rh()) && ACCEPTE.equals(e.finance()) ? e.direction() : BLOQUE;
        };
    }

    private Etape etapeDetail(String code, String libelle, Etapes etapes, Map<String, ValidationBesoin> courantes) {
        String etat = etatAffiche(code, etapes);
        ValidationBesoin v = courantes.get(code);
        boolean decidee = v != null && !BLOQUE.equals(etat) && !EN_ATTENTE.equals(v.getDecision().getCode());
        if (!decidee) {
            return new Etape(code, libelle, etat, null, null, null);
        }
        Utilisateur validateur = v.getValidateur();
        String nom = validateur == null ? null : validateur.getPrenom() + " " + validateur.getNom();
        return new Etape(code, libelle, etat, nom, v.getDateDecision(), v.getRemarque());
    }

    /** Coût annuel de la demande, ou null tant que les RH n'ont pas défini le salaire estimé. */
    private BigDecimal coutTotal(Besoin b) {
        if (b.getSalaireEstime() == null) {
            return null;
        }
        return b.getSalaireEstime()
                .multiply(BigDecimal.valueOf(b.getEffectif()))
                .multiply(BigDecimal.valueOf(NB_MOIS));
    }

    private BigDecimal restantDe(LigneDepartement budget) {
        return budget == null ? BigDecimal.ZERO : budget.restant();
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

    private LigneDepartement budgetDe(Besoin b) {
        return budgetParDepartement(anneeBudget(b)).get(b.getPoste().getDepartement().getId());
    }

    /** Grille salariale du poste en vigueur aujourd'hui, ou null s'il n'y en a pas. */
    private GrilleSalaire grilleDe(Besoin b) {
        return grilleRepository.findEnVigueur(b.getPoste().getId(), LocalDate.now())
                .stream().findFirst().orElse(null);
    }

    // =====================================================================
    // Accès base
    // =====================================================================

    private Besoin chargerPourMiseAJour(Integer idBesoin) {
        return besoinRepository.findByIdPourMiseAJour(idBesoin)
                .orElseThrow(() -> new RegleMetierException(HttpStatus.NOT_FOUND, "Demande introuvable."));
    }

    private Utilisateur chargerUtilisateur(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RegleMetierException(HttpStatus.UNAUTHORIZED, "Utilisateur non reconnu."));
    }

    private boolean estFinance(Utilisateur utilisateur) {
        return FINANCE.equalsIgnoreCase(utilisateur.getRole().getCode());
    }

    private void exigerFinance(Utilisateur utilisateur) {
        if (!estFinance(utilisateur)) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN,
                    "Seule la Finance peut valider ou refuser cette demande.");
        }
    }

    /** Décision courante de chaque étape (la plus récente) pour un seul besoin. */
    private Map<String, ValidationBesoin> courantesDuBesoin(Integer idBesoin) {
        return validationsCourantes(List.of(idBesoin)).getOrDefault(idBesoin, Map.of());
    }

    /** { idBesoin -> { étape -> décision courante } } */
    private Map<Integer, Map<String, ValidationBesoin>> validationsCourantes(Collection<Integer> ids) {
        Map<Integer, Map<String, ValidationBesoin>> resultat = new HashMap<>();
        for (ValidationBesoin v : validationRepository.findAllForBesoins(ids)) {
            resultat.computeIfAbsent(v.getBesoin().getId(), k -> new HashMap<>())
                    .merge(v.getEtatValidation().getCode(), v, DemandeFinanceService::plusRecente);
        }
        return resultat;
    }

    private static ValidationBesoin plusRecente(ValidationBesoin a, ValidationBesoin b) {
        return ORDRE_RECENCE.compare(b, a) > 0 ? b : a;
    }

    /**
     * AJOUTE une décision à l'historique (jamais de modification d'une ligne existante).
     * Le trigger SQL exige un validateur et une date pour ACCEPTE / REFUSE.
     */
    private void enregistrer(Besoin besoin, String etape, String codeDecision,
                             Utilisateur validateur, String remarque) {
        ValidationBesoin v = new ValidationBesoin();
        v.setBesoin(besoin);
        v.setEtatValidation(etatParCode(etape));
        v.setDecision(decisionParCode(codeDecision));
        v.setValidateur(validateur);
        v.setDateDecision(OffsetDateTime.now());
        v.setRemarque(remarque);
        validationRepository.saveAndFlush(v);
    }

    private EtatValidation etatParCode(String code) {
        return etatRepository.findByCode(code).orElseThrow(() ->
                new RegleMetierException(HttpStatus.INTERNAL_SERVER_ERROR, "Étape de validation inconnue : " + code));
    }

    private Decision decisionParCode(String code) {
        return decisionRepository.findByCode(code).orElseThrow(() ->
                new RegleMetierException(HttpStatus.INTERNAL_SERVER_ERROR, "Décision inconnue : " + code));
    }

    // =====================================================================
    // Utilitaires
    // =====================================================================

    private String texteOuNull(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }

    /** 1500000 -> « 1 500 000 Ar » (même rendu que le front). */
    private String ariary(BigDecimal montant) {
        String nombre = NumberFormat.getIntegerInstance(Locale.FRANCE).format(montant)
                .replace('\u202f', ' ').replace('\u00a0', ' ');
        return nombre + " Ar";
    }
}
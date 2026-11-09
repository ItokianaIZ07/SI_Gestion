// BY : Brandon
package com.si.rh.service;

import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.*;
import com.si.rh.model.enums.*;
import com.si.rh.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PointageService {

    private final PointageRepository pointageRepo;
    private final TempsTravailJournalierRepository tempsRepo;
    private final AnomaliePointageRepository anomalieRepo;
    private final EmployeRepository employeRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final PlanningTravailRepository planningRepo;
    private final JourFerieRepository jourFerieRepo;
    private final AbsenceRepository absenceRepo;
    private final MissionRepository missionRepo;

    public PointageService(
            PointageRepository pointageRepo,
            TempsTravailJournalierRepository tempsRepo,
            AnomaliePointageRepository anomalieRepo,
            EmployeRepository employeRepo,
            UtilisateurRepository utilisateurRepo,
            PlanningTravailRepository planningRepo,
            JourFerieRepository jourFerieRepo,
            AbsenceRepository absenceRepo,
            MissionRepository missionRepo) {
        this.pointageRepo = pointageRepo;
        this.tempsRepo = tempsRepo;
        this.anomalieRepo = anomalieRepo;
        this.employeRepo = employeRepo;
        this.utilisateurRepo = utilisateurRepo;
        this.planningRepo = planningRepo;
        this.jourFerieRepo = jourFerieRepo;
        this.absenceRepo = absenceRepo;
        this.missionRepo = missionRepo;
    }

    // ---------------------------------------------------------
    // SÉCURITÉ
    // ---------------------------------------------------------

    public Utilisateur getUtilisateur(String email) {
        return utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable."));
    }

    private Integer getIdEmployeManager(Utilisateur u) {
        return employeRepo.findAll().stream()
                .filter(e -> e.getUtilisateur() != null && e.getUtilisateur().getId().equals(u.getId()))
                .findFirst()
                .map(Employe::getId)
                .orElse(null);
    }

    public void verifierAcces(Integer idEmploye, Utilisateur u) {
        String role = u.getRole().getCode();
        if ("RH".equals(role) || "DIRECTION".equals(role)) return;

        if ("MANAGER".equals(role)) {
            Integer idEmployeManager = getIdEmployeManager(u);
            if (idEmployeManager == null) {
                throw new RegleMetierException(HttpStatus.FORBIDDEN,
                        "Votre compte manager n'est associé à aucun employé. Contactez le RH.");
            }
            if (!employeRepo.estSubordonneDe(idEmploye, idEmployeManager)) {
                throw new RegleMetierException(HttpStatus.FORBIDDEN,
                        "Vous ne pouvez voir que les employés de votre équipe.");
            }
            return;
        }
        throw new RegleMetierException(HttpStatus.FORBIDDEN, "Accès refusé.");
    }

    // ---------------------------------------------------------
    // POINTAGE — RÈGLES DE SÉQUENCE
    // ---------------------------------------------------------

    public Pointage enregistrer(Integer idEmploye, TypePointage type, OffsetDateTime dateHeure, String email) {
        Utilisateur u = getUtilisateur(email);
        if (!"RH".equals(u.getRole().getCode())) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN, "Seul le RH peut pointer.");
        }

        Employe e = employeRepo.findById(idEmploye)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable."));
        OffsetDateTime dt = dateHeure != null ? dateHeure : OffsetDateTime.now();

        LocalDate jourPointage = dt.atZoneSameInstant(ZoneId.of("Indian/Antananarivo")).toLocalDate();
        OffsetDateTime start = jourPointage.atStartOfDay(ZoneId.of("Indian/Antananarivo")).toOffsetDateTime();
        OffsetDateTime end = jourPointage.plusDays(1).atStartOfDay(ZoneId.of("Indian/Antananarivo")).toOffsetDateTime();
        List<Pointage> pointagesDuJour = pointageRepo
                .findByEmploye_IdAndDateHeureBetweenOrderByDateHeureAsc(idEmploye, start, end);

        TypePointage dernierType = pointagesDuJour.isEmpty()
                ? null
                : pointagesDuJour.get(pointagesDuJour.size() - 1).getTypePointage();

        // === RÈGLE 1 : ENTREE ne peut suivre que SORTIE ou NULL ===
        if (type == TypePointage.ENTREE) {
            if (dernierType != null && dernierType != TypePointage.SORTIE) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas pointer une entrée alors que le dernier pointage est " +
                    dernierType + ". Vous devez d'abord pointer une sortie.");
            }
        }

        // === RÈGLE 2 : SORTIE ne peut suivre que ENTREE ===
        if (type == TypePointage.SORTIE) {
            if (dernierType != TypePointage.ENTREE) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas pointer une sortie sans avoir pointé une entrée au préalable.");
            }
        }

        // === RÈGLE 3 : DEBUT_PAUSE seulement après ENTREE ===
        if (type == TypePointage.DEBUT_PAUSE) {
            if (dernierType != TypePointage.ENTREE) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas pointer un début de pause sans avoir pointé une entrée.");
            }
        }

        // === RÈGLE 4 : FIN_PAUSE seulement après DEBUT_PAUSE ===
        if (type == TypePointage.FIN_PAUSE) {
            if (dernierType != TypePointage.DEBUT_PAUSE) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Vous devez d'abord pointer un début de pause avant de pointer une fin de pause.");
            }
        }

        // Vérif doublon
        if (pointageRepo.existsByEmploye_IdAndDateHeureAndTypePointage(idEmploye, dt, type)) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Ce pointage existe déjà à cet instant.");
        }

        Pointage p = new Pointage();
        p.setEmploye(e);
        p.setTypePointage(type);
        p.setDateHeure(dt);
        Pointage saved = pointageRepo.save(p);
        pointageRepo.flush();

        // === RÈGLE 5 : Anomalie SORTIE_ANTICIPEE seulement sur la 1ère sortie ===
        if (type == TypePointage.SORTIE) {
            long nbSortiesAvant = pointagesDuJour.stream()
                    .filter(pt -> pt.getTypePointage() == TypePointage.SORTIE)
                    .count();
            
            if (nbSortiesAvant == 0) {  // Seulement la 1ère sortie
                Optional<PlanningTravail> planningOpt = planningRepo.findByEmployeAndDate(idEmploye, jourPointage);
                if (planningOpt.isPresent() && planningOpt.get().getHoraire() != null) {
                    HoraireTravail horaire = planningOpt.get().getHoraire();
                    LocalTime heureSortie = dt.atZoneSameInstant(ZoneId.of("Indian/Antananarivo")).toLocalTime();
                    if (heureSortie.isBefore(horaire.getHeureFin())) {
                        long minutesAnticipees = Duration.between(heureSortie, horaire.getHeureFin()).toMinutes();
                        creerAnomalieSiAbsente(e, jourPointage, TypeAnomalie.SORTIE_ANTICIPEE,
                            "Sortie anticipée de " + minutesAnticipees + " minute(s) avant l'heure prévue (" +
                            horaire.getHeureFin() + ").");
                    }
                }
            }
        }

        return saved;
    }

    public List<Pointage> pointagesDuJour(Integer idEmploye, LocalDate jour, String email) {
        verifierAcces(idEmploye, getUtilisateur(email));
        OffsetDateTime start = jour.atStartOfDay(ZoneId.of("Indian/Antananarivo")).toOffsetDateTime();
        OffsetDateTime end = jour.plusDays(1).atStartOfDay(ZoneId.of("Indian/Antananarivo")).toOffsetDateTime();
        return pointageRepo.findByEmploye_IdAndDateHeureBetweenOrderByDateHeureAsc(idEmploye, start, end);
    }

    // ---------------------------------------------------------
    // MOTEUR DE CALCUL
    // ---------------------------------------------------------

    public TempsTravailJournalier recalculerJournee(Integer idEmploye, LocalDate date, String email) {
        verifierAcces(idEmploye, getUtilisateur(email));
        Employe employe = employeRepo.findById(idEmploye)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable."));

        OffsetDateTime start = date.atStartOfDay(ZoneId.of("Indian/Antananarivo")).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneId.of("Indian/Antananarivo")).toOffsetDateTime();
        List<Pointage> pointages = pointageRepo
                .findByEmploye_IdAndDateHeureBetweenOrderByDateHeureAsc(idEmploye, start, end);

        Optional<PlanningTravail> planningOpt = planningRepo.findByEmployeAndDate(idEmploye, date);
        HoraireTravail horaire = planningOpt.map(PlanningTravail::getHoraire).orElse(null);

        boolean estFerie = jourFerieRepo.findById(date).isPresent();
        boolean estAbsent = absenceRepo.existsByEmployeAndDate(idEmploye, date);
        boolean estEnMission = missionRepo.existsByEmployeAndDate(idEmploye, date);

        int tempsPrevu = 0;
        if (horaire != null && !estFerie && !estAbsent && !estEnMission) {
            int dureeJournee = (int) Duration.between(horaire.getHeureDebut(), horaire.getHeureFin()).toMinutes();
            int pause = (horaire.getDebutPause() != null)
                    ? (int) Duration.between(horaire.getDebutPause(), horaire.getFinPause()).toMinutes()
                    : 0;
            tempsPrevu = dureeJournee - pause;
        }

        // === Séquence valide ===
        boolean sequenceValide = true;
        TypePointage precedent = null;
        for (Pointage p : pointages) {
            if (precedent == null) {
                if (p.getTypePointage() != TypePointage.ENTREE) sequenceValide = false;
            } else {
                boolean transitionOK =
                        (precedent == TypePointage.ENTREE && (p.getTypePointage() == TypePointage.DEBUT_PAUSE || p.getTypePointage() == TypePointage.SORTIE))
                        || (precedent == TypePointage.DEBUT_PAUSE && p.getTypePointage() == TypePointage.FIN_PAUSE)
                        || (precedent == TypePointage.FIN_PAUSE && p.getTypePointage() == TypePointage.SORTIE)
                        || (precedent == TypePointage.SORTIE && p.getTypePointage() == TypePointage.ENTREE);
                if (!transitionOK) sequenceValide = false;
            }
            precedent = p.getTypePointage();
        }

        // === Temps réel cumulé (somme des paires ENTRÉE→SORTIE ou ENTRÉE→DEBUT_PAUSE ou FIN_PAUSE→SORTIE) ===
        int tempsReel = 0;
        Pointage ouverture = null;
        for (Pointage p : pointages) {
            if (p.getTypePointage() == TypePointage.ENTREE || p.getTypePointage() == TypePointage.FIN_PAUSE) {
                ouverture = p;
            } else if ((p.getTypePointage() == TypePointage.DEBUT_PAUSE || p.getTypePointage() == TypePointage.SORTIE)
                    && ouverture != null) {
                tempsReel += (int) Duration.between(ouverture.getDateHeure(), p.getDateHeure()).toMinutes();
                ouverture = null;
            }
        }

        // === Retard : seulement sur la 1ère ENTRÉE ===
        int retard = 0;
        Optional<Pointage> premiereEntree = pointages.stream()
                .filter(p -> p.getTypePointage() == TypePointage.ENTREE).findFirst();
        if (premiereEntree.isPresent() && horaire != null) {
            LocalTime heureArrivee = premiereEntree.get().getDateHeure()
                    .atZoneSameInstant(ZoneId.of("Indian/Antananarivo")).toLocalTime();
            long minutesRetard = Duration.between(horaire.getHeureDebut(), heureArrivee).toMinutes();
            if (minutesRetard > horaire.getToleranceRetardMinutes()) {
                retard = (int) minutesRetard;
            }
        }

        // === Heures sup : temps réel au-delà du temps prévu ===
        int heuresSup = Math.max(0, tempsReel - tempsPrevu);

        TempsTravailJournalier t = tempsRepo.findByEmploye_IdAndDateTravail(idEmploye, date)
                .orElseGet(() -> {
                    TempsTravailJournalier nouveau = new TempsTravailJournalier();
                    nouveau.setEmploye(employe);
                    nouveau.setDateTravail(date);
                    return nouveau;
                });

        StatutHeuresSup statut = StatutHeuresSup.AUCUNE;
        if (heuresSup > 0) {
            if (t.getStatutHeuresSup() == StatutHeuresSup.VALIDEE
                    || t.getStatutHeuresSup() == StatutHeuresSup.REFUSEE) {
                statut = (t.getHeuresSupplementairesMinutes() != null
                        && t.getHeuresSupplementairesMinutes() == heuresSup)
                        ? t.getStatutHeuresSup()
                        : StatutHeuresSup.A_VALIDER;
            } else {
                statut = StatutHeuresSup.A_VALIDER;
            }
        }

        t.setTempsPrevuMinutes(tempsPrevu);
        t.setTempsReelMinutes(tempsReel);
        t.setRetardMinutes(retard);
        t.setHeuresSupplementairesMinutes(heuresSup);
        t.setStatutHeuresSup(statut);
        tempsRepo.save(t);
        tempsRepo.flush();

        genererAnomalies(employe, date, pointages, sequenceValide, tempsPrevu, retard);

        return t;
    }

    private void genererAnomalies(Employe e, LocalDate date, List<Pointage> pointages,
                                  boolean sequenceValide, int tempsPrevu, int retard) {
        LocalDate aujourdhui = LocalDate.now();

        if (tempsPrevu > 0 && pointages.isEmpty() && date.isBefore(aujourdhui)) {
            creerAnomalieSiAbsente(e, date, TypeAnomalie.JOUR_NON_POINTE,
                    "Aucun pointage sur un jour de travail.");
        }

        if (!pointages.isEmpty()
                && pointages.get(pointages.size() - 1).getTypePointage() != TypePointage.SORTIE
                && date.isBefore(aujourdhui)) {
            creerAnomalieSiAbsente(e, date, TypeAnomalie.SORTIE_MANQUANTE,
                    "La journée ne se termine pas par une sortie. Dernier pointage : " +
                    pointages.get(pointages.size() - 1).getTypePointage() + " à " +
                    pointages.get(pointages.size() - 1).getDateHeure().toLocalTime() + ".");
        }

        if (!sequenceValide) {
            String cause = trouverCauseSequenceInvalide(pointages);
            creerAnomalieSiAbsente(e, date, TypeAnomalie.SEQUENCE_INVALIDE, cause);
        }

        if (retard > 0) {
            creerAnomalieSiAbsente(e, date, TypeAnomalie.RETARD,
                    "Retard de " + retard + " minute(s).");
        }
    }

    private String trouverCauseSequenceInvalide(List<Pointage> pointages) {
        if (pointages.isEmpty()) return "Aucun pointage.";

        TypePointage precedent = null;
        Pointage pointPrecedent = null;

        for (Pointage p : pointages) {
            if (precedent == null) {
                if (p.getTypePointage() != TypePointage.ENTREE) {
                    return "Le premier pointage de la journée est " + p.getTypePointage() +
                           " à " + p.getDateHeure().toLocalTime() + " (devrait être ENTREE).";
                }
            } else {
                boolean transitionOK =
                        (precedent == TypePointage.ENTREE && (p.getTypePointage() == TypePointage.DEBUT_PAUSE || p.getTypePointage() == TypePointage.SORTIE))
                        || (precedent == TypePointage.DEBUT_PAUSE && p.getTypePointage() == TypePointage.FIN_PAUSE)
                        || (precedent == TypePointage.FIN_PAUSE && p.getTypePointage() == TypePointage.SORTIE)
                        || (precedent == TypePointage.SORTIE && p.getTypePointage() == TypePointage.ENTREE);

                if (!transitionOK) {
                    return "Pointage " + p.getTypePointage() + " à " + p.getDateHeure().toLocalTime() +
                           " ne peut pas suivre " + precedent + " à " + pointPrecedent.getDateHeure().toLocalTime() + ".";
                }
            }
            precedent = p.getTypePointage();
            pointPrecedent = p;
        }
        return "Séquence invalide (cause inconnue).";
    }

    private void creerAnomalieSiAbsente(Employe e, LocalDate date, TypeAnomalie type, String description) {
        if (!anomalieRepo.existsByEmploye_IdAndDateAnomalieAndTypeAnomalie(e.getId(), date, type)) {
            AnomaliePointage a = new AnomaliePointage();
            a.setEmploye(e);
            a.setDateAnomalie(date);
            a.setTypeAnomalie(type);
            a.setDescription(description);
            a.setStatut(StatutAnomalie.A_TRAITER);
            anomalieRepo.save(a);
            anomalieRepo.flush();
        }
    }

    // ---------------------------------------------------------
    // HEURES SUPPLÉMENTAIRES
    // ---------------------------------------------------------

    public List<TempsTravailJournalier> heuresSupAValider(String email) {
        Utilisateur u = getUtilisateur(email);
        List<TempsTravailJournalier> toutes = tempsRepo.findByStatutHeuresSupOrderByDateTravailDesc(StatutHeuresSup.A_VALIDER);

        if ("RH".equals(u.getRole().getCode()) || "DIRECTION".equals(u.getRole().getCode())) {
            return toutes;
        }
        if ("MANAGER".equals(u.getRole().getCode())) {
            Integer idEmployeManager = getIdEmployeManager(u);
            if (idEmployeManager == null) return List.of();
            return toutes.stream()
                    .filter(t -> employeRepo.estSubordonneDe(t.getEmploye().getId(), idEmployeManager))
                    .toList();
        }
        throw new RegleMetierException(HttpStatus.FORBIDDEN, "Accès refusé.");
    }

    public void validerHeuresSup(Integer idTemps, StatutHeuresSup decision, String email) {
        Utilisateur u = getUtilisateur(email);
        TempsTravailJournalier t = tempsRepo.findById(idTemps)
                .orElseThrow(() -> new RessourceIntrouvableException("Introuvable."));

        if (t.getStatutHeuresSup() != StatutHeuresSup.A_VALIDER) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ces heures supplémentaires ont déjà été traitées.");
        }

        if ("MANAGER".equals(u.getRole().getCode())) {
            Integer idEmployeManager = getIdEmployeManager(u);
            if (idEmployeManager == null || !employeRepo.estSubordonneDe(t.getEmploye().getId(), idEmployeManager)) {
                throw new RegleMetierException(HttpStatus.FORBIDDEN, "Hors de votre équipe.");
            }
        } else if (!"RH".equals(u.getRole().getCode()) && !"DIRECTION".equals(u.getRole().getCode())) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }

        t.setStatutHeuresSup(decision);
        t.setValidateur(u);
        t.setDateValidation(OffsetDateTime.now());
        tempsRepo.save(t);
        tempsRepo.flush();
    }

    // ---------------------------------------------------------
    // ANOMALIES
    // ---------------------------------------------------------

    public List<AnomaliePointage> anomalies(String email) {
        Utilisateur u = getUtilisateur(email);
        List<AnomaliePointage> toutes = anomalieRepo.findAll();

        if ("RH".equals(u.getRole().getCode()) || "DIRECTION".equals(u.getRole().getCode())) {
            return toutes;
        }
        if ("MANAGER".equals(u.getRole().getCode())) {
            Integer idEmployeManager = getIdEmployeManager(u);
            if (idEmployeManager == null) return List.of();
            return toutes.stream()
                    .filter(a -> employeRepo.estSubordonneDe(a.getEmploye().getId(), idEmployeManager))
                    .toList();
        }
        throw new RegleMetierException(HttpStatus.FORBIDDEN, "Accès refusé.");
    }

    public AnomaliePointage resoudreAnomalie(Integer id, String justification, String email) {
        Utilisateur u = getUtilisateur(email);
        if (!"RH".equals(u.getRole().getCode())) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN, "Seul le RH peut résoudre une anomalie.");
        }

        AnomaliePointage a = anomalieRepo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Anomalie introuvable."));

        if (a.getStatut() == StatutAnomalie.RESOLUE) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Cette anomalie est déjà résolue.");
        }
        if (justification == null || justification.isBlank()) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La justification est obligatoire pour résoudre une anomalie.");
        }

        a.setStatut(StatutAnomalie.RESOLUE);
        a.setJustification(justification.trim());
        a.setDateResolution(OffsetDateTime.now());
        a.setResponsable(u);
        anomalieRepo.save(a);
        anomalieRepo.flush();
        return a;
    }

    // ---------------------------------------------------------
    // RÉCAPITULATIF MENSUEL
    // ---------------------------------------------------------

    public Map<String, Object> recapitulatifMensuel(Integer idDepartement, int annee, int mois, String email) {
        Utilisateur u = getUtilisateur(email);
        if (!"RH".equals(u.getRole().getCode()) && !"DIRECTION".equals(u.getRole().getCode())) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN, "Seul le RH peut voir le récapitulatif.");
        }

        LocalDate debut = LocalDate.of(annee, mois, 1);
        LocalDate fin = debut.withDayOfMonth(debut.lengthOfMonth());

        List<TempsTravailJournalier> lignes = tempsRepo
                .findByEmploye_Departement_IdAndDateTravailBetweenOrderByEmploye_NomAsc(idDepartement, debut, fin);

        Map<Integer, List<TempsTravailJournalier>> parEmploye = lignes.stream()
                .collect(Collectors.groupingBy(t -> t.getEmploye().getId()));

        List<Map<String, Object>> resultat = parEmploye.values().stream().map(liste -> {
            Employe emp = liste.get(0).getEmploye();

            int totalPrevu = liste.stream().mapToInt(TempsTravailJournalier::getTempsPrevuMinutes).sum();
            int totalReel = liste.stream().mapToInt(TempsTravailJournalier::getTempsReelMinutes).sum();
            int totalRetard = liste.stream().mapToInt(TempsTravailJournalier::getRetardMinutes).sum();
            long nbRetards = liste.stream().filter(t -> t.getRetardMinutes() > 0).count();
            int hsValidees = liste.stream()
                    .filter(t -> t.getStatutHeuresSup() == StatutHeuresSup.VALIDEE)
                    .mapToInt(TempsTravailJournalier::getHeuresSupplementairesMinutes).sum();
            int hsAValider = liste.stream()
                    .filter(t -> t.getStatutHeuresSup() == StatutHeuresSup.A_VALIDER)
                    .mapToInt(TempsTravailJournalier::getHeuresSupplementairesMinutes).sum();

            return Map.<String, Object>of(
                    "employeId", emp.getId(),
                    "nom", emp.getNom(),
                    "prenom", emp.getPrenom(),
                    "matricule", emp.getMatricule(),
                    "tempsPrevu", totalPrevu,
                    "tempsReel", totalReel,
                    "retardTotal", totalRetard,
                    "nbRetards", nbRetards,
                    "hsValidees", hsValidees,
                    "hsAValider", hsAValider
            );
        }).collect(Collectors.toList());

        return Map.of("annee", annee, "mois", mois, "lignes", resultat);
    }
}
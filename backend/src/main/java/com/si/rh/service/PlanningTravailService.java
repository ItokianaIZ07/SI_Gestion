package com.si.rh.service;

import com.si.rh.dto.rh.planningTravail.PlanningTravailAvecDetails;
import com.si.rh.dto.rh.planningTravail.PlanningTravailRequest;
import com.si.rh.dto.rh.planningTravail.EmployePlanningFiche;
import com.si.rh.dto.rh.planningTravail.PlanningApercu;
import com.si.rh.dto.rh.planningTravail.PlanningOptions;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Employe;
import com.si.rh.model.HoraireTravail;
import com.si.rh.model.PlanningTravail;
import com.si.rh.model.RegimeTravail;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.HoraireTravailRepository;
import com.si.rh.repository.PlanningTravailRepository;
import com.si.rh.repository.RegimeTravailRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlanningTravailService {

    private final PlanningTravailRepository planningTravailRepository;
    private final EmployeRepository employeRepository;
    private final HoraireTravailRepository horaireTravailRepository;
    private final RegimeTravailRepository regimeTravailRepository;
    private final DepartementRepository departementRepository;
    private final UtilisateurRepository utilisateurRepository;

    public PlanningTravailService(
            PlanningTravailRepository planningTravailRepository,
            EmployeRepository employeRepository,
            HoraireTravailRepository horaireTravailRepository,
            RegimeTravailRepository regimeTravailRepository,
            DepartementRepository departementRepository,
            UtilisateurRepository utilisateurRepository) {
        this.planningTravailRepository = planningTravailRepository;
        this.employeRepository = employeRepository;
        this.horaireTravailRepository = horaireTravailRepository;
        this.regimeTravailRepository = regimeTravailRepository;
        this.departementRepository = departementRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanningTravailAvecDetails> lister(
            Integer idEmploye,
            Integer idDepartement,
            LocalDate dateDebut,
            LocalDate dateFin,
            Integer idHoraire) {
        validerFiltres(dateDebut, dateFin);
        return planningTravailRepository.findAllAvecFiltres(
                idEmploye, idDepartement, idHoraire, dateDebut, dateFin);
    }

    @Transactional(readOnly = true)
    public List<PlanningTravailAvecDetails> listerPourUtilisateur(
            Integer idEmploye,
            Integer idDepartement,
            LocalDate dateDebut,
            LocalDate dateFin,
            Integer idHoraire,
            String emailUtilisateur) {
        validerFiltres(dateDebut, dateFin);
        var utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable."));
        String role = utilisateur.getRole().getCode();
        if ("MANAGER".equals(role)) {
            return planningTravailRepository.findAllAvecFiltresManager(
                    utilisateur.getId(), idEmploye, idDepartement, idHoraire, dateDebut, dateFin);
        }
        if ("RH".equals(role) || "DIRECTION".equals(role)) {
            return planningTravailRepository.findAllAvecFiltres(
                    idEmploye, idDepartement, idHoraire, dateDebut, dateFin);
        }
        throw new RegleMetierException(HttpStatus.FORBIDDEN, "Accès refusé.");
    }

    @Transactional(readOnly = true)
    public List<PlanningTravailAvecDetails> historique(Integer idEmploye) {
        return planningTravailRepository.findAllAvecFiltres(idEmploye, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<PlanningTravailAvecDetails> historiquePourUtilisateur(Integer idEmploye, String emailUtilisateur) {
        var utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable."));
        if ("MANAGER".equals(utilisateur.getRole().getCode())) {
            verifierAccesEquipeManager(utilisateur.getId(), idEmploye);
            return planningTravailRepository.findAllAvecFiltresManager(
                    utilisateur.getId(), idEmploye, null, null, null, null);
        }
        if ("RH".equals(utilisateur.getRole().getCode()) || "DIRECTION".equals(utilisateur.getRole().getCode())) {
            return historique(idEmploye);
        }
        throw new RegleMetierException(HttpStatus.FORBIDDEN, "Accès refusé.");
    }

    @Transactional(readOnly = true)
    public EmployePlanningFiche ficheEmploye(Integer idEmploye) {
        Employe employe = employeRepository.findById(idEmploye)
                .orElseThrow(() -> new RessourceIntrouvableException("Salarié introuvable : " + idEmploye));
        return new EmployePlanningFiche(
                employe.getId(),
                employe.getMatricule(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getDepartement().getNom(),
                employe.getStatut());
    }

    @Transactional(readOnly = true)
    public EmployePlanningFiche ficheEmployePourUtilisateur(Integer idEmploye, String emailUtilisateur) {
        var utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable."));
        if ("MANAGER".equals(utilisateur.getRole().getCode())) {
            verifierAccesEquipeManager(utilisateur.getId(), idEmploye);
        } else if (!"RH".equals(utilisateur.getRole().getCode())
                && !"DIRECTION".equals(utilisateur.getRole().getCode())) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
        return ficheEmploye(idEmploye);
    }

    @Transactional(readOnly = true)
    public PlanningOptions options() {
        List<PlanningOptions.EmployeOption> employes =
                employeRepository.findByStatutInOrderByNomAscPrenomAsc(
                                List.of(StatutEmploye.ACTIF, StatutEmploye.SUSPENDU)).stream()
                        .map(employe -> new PlanningOptions.EmployeOption(
                                employe.getId(), employe.getMatricule(), employe.getNom(), employe.getPrenom()))
                        .toList();
        List<PlanningOptions.DepartementOption> departements =
                departementRepository.findAllByOrderByNomAsc().stream()
                        .map(departement -> new PlanningOptions.DepartementOption(
                                departement.getId(), departement.getNom()))
                        .toList();
        List<PlanningOptions.HoraireOption> horaires =
                horaireTravailRepository.findByActifTrueOrderByLibelleAsc().stream()
                        .map(horaire -> new PlanningOptions.HoraireOption(
                                horaire.getId(), horaire.getCode(), horaire.getLibelle()))
                        .toList();
        List<PlanningOptions.RegimeOption> regimes =
                regimeTravailRepository.findAllByOrderByLibelleAsc().stream()
                        .map(regime -> new PlanningOptions.RegimeOption(
                                regime.getId(), regime.getCode(), regime.getLibelle()))
                        .toList();
        return new PlanningOptions(employes, departements, horaires, regimes);
    }

    @Transactional(readOnly = true)
    public List<PlanningOptions.DepartementOption> departements() {
        return departementRepository.findAllByOrderByNomAsc().stream()
                .map(departement -> new PlanningOptions.DepartementOption(
                        departement.getId(), departement.getNom()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanningApercu apercuAffectation(Integer idEmploye, LocalDate dateDebut) {
        if (idEmploye == null || dateDebut == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le salarié et la date de début sont obligatoires pour l'aperçu.");
        }
        LocalDate dateFinPlanningActuel = planningTravailRepository
                .findFirstByEmploye_IdAndDateFinIsNullAndDateDebutBeforeOrderByDateDebutDesc(idEmploye, dateDebut)
                .map(planning -> dateDebut.minusDays(1))
                .orElse(null);
        return new PlanningApercu(dateFinPlanningActuel);
    }

    @Transactional(readOnly = true)
    public Optional<HoraireTravail> getHoraireEnVigueur(Integer idEmploye, LocalDate date) {
        if (idEmploye == null || date == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le salarié et la date sont obligatoires pour rechercher l'horaire en vigueur.");
        }
        return planningTravailRepository.findHoraireEnVigueur(idEmploye, date);
    }

    @Transactional(readOnly = true)
    public Optional<PlanningTravail> getById(Integer id) {
        return planningTravailRepository.findById(id);
    }

    public PlanningTravail affecter(PlanningTravailRequest demande) {
        valider(demande);
        Employe employe = getEmployeValide(demande.idEmploye(), demande.dateDebut());
        HoraireTravail horaire = getHoraireActif(demande.idHoraire());
        RegimeTravail regime = getRegime(demande.idRegime());

        verifierChevauchement(employe.getId(), demande, null, true);
        planningTravailRepository.cloturerPlanningsOuvertsAnterieurs(
                employe.getId(), demande.dateDebut(), demande.dateDebut().minusDays(1));

        PlanningTravail planning = new PlanningTravail();
        planning.setEmploye(employe);
        planning.setHoraire(horaire);
        planning.setRegime(regime);
        planning.setDateDebut(demande.dateDebut());
        planning.setDateFin(demande.dateFin());
        return enregistrerAvecControleChevauchement(planning);
    }

    public PlanningTravail modifier(Integer id, PlanningTravailRequest demande) {
        valider(demande);
        PlanningTravail planning = planningTravailRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Planning introuvable : " + id));
        if (!planning.getEmploye().getId().equals(demande.idEmploye())) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le salarié d'un planning ne peut pas être modifié.");
        }

        Employe employe = getEmployeValide(demande.idEmploye(), demande.dateDebut());
        verifierChevauchement(employe.getId(), demande, id, false);
        planning.setHoraire(getHoraireActif(demande.idHoraire()));
        planning.setRegime(getRegime(demande.idRegime()));
        planning.setDateDebut(demande.dateDebut());
        planning.setDateFin(demande.dateFin());
        return enregistrerAvecControleChevauchement(planning);
    }

    public void supprimer(Integer id) {
        PlanningTravail planning = planningTravailRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Planning introuvable : " + id));

        if (!planning.getDateDebut().isAfter(LocalDate.now())) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Seuls les plannings futurs peuvent être supprimés.");
        }

        long journeesCalculees = planningTravailRepository.countJourneesCalculees(
                planning.getEmploye().getId(), planning.getDateDebut(), planning.getDateFin());
        if (journeesCalculees > 0) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Impossible de supprimer ce planning : " + journeesCalculees
                            + " journée(s) calculée(s) y sont déjà associée(s).");
        }

        planningTravailRepository.delete(planning);
    }

    private PlanningTravail enregistrerAvecControleChevauchement(PlanningTravail planning) {
        try {
            return planningTravailRepository.saveAndFlush(planning);
        } catch (DataIntegrityViolationException exception) {
            if (estChevauchementPeriode(exception)) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                        "La période du planning chevauche un autre planning du salarié.");
            }
            throw exception;
        }
    }

    private void verifierChevauchement(
            Integer idEmploye,
            PlanningTravailRequest demande,
            Integer idIgnore,
            boolean autoriserCloture) {
        planningTravailRepository.findChevauchement(
                        idEmploye, idIgnore, demande.dateDebut(), demande.dateFin(), autoriserCloture)
                .ifPresent(existant -> {
                    String fin = existant.getDateFin() == null
                            ? "sans date de fin"
                            : "au " + existant.getDateFin();
                    throw new RegleMetierException(HttpStatus.CONFLICT,
                            "Le planning chevauche un planning existant du "
                                    + existant.getDateDebut() + " " + fin + ".");
                });
    }

    private boolean estChevauchementPeriode(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("ex_planning_periode")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private Employe getEmployeValide(Integer idEmploye, LocalDate dateDebut) {
        Employe employe = employeRepository.findById(idEmploye)
                .orElseThrow(() -> new RessourceIntrouvableException("Salarié introuvable : " + idEmploye));
        if (employe.getStatut() == StatutEmploye.PARTI) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Un salarié parti ne peut pas recevoir de planning.");
        }
        if (employe.getDateEmbauche() == null || dateDebut.isBefore(employe.getDateEmbauche())) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La date de début du planning doit être égale ou postérieure à la date d'embauche.");
        }
        return employe;
    }

    private HoraireTravail getHoraireActif(Integer idHoraire) {
        HoraireTravail horaire = horaireTravailRepository.findById(idHoraire)
                .orElseThrow(() -> new RessourceIntrouvableException("Horaire introuvable : " + idHoraire));
        if (!Boolean.TRUE.equals(horaire.getActif())) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Un horaire inactif ne peut pas être affecté à un planning.");
        }
        return horaire;
    }

    private RegimeTravail getRegime(Integer idRegime) {
        if (idRegime == null) {
            return null;
        }
        return regimeTravailRepository.findById(idRegime)
                .orElseThrow(() -> new RessourceIntrouvableException("Régime introuvable : " + idRegime));
    }

    private void valider(PlanningTravailRequest demande) {
        if (demande == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Les données du planning sont obligatoires.");
        }
        if (demande.idEmploye() == null || demande.idHoraire() == null || demande.dateDebut() == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le salarié, l'horaire et la date de début sont obligatoires.");
        }
        if (demande.dateFin() != null && demande.dateFin().isBefore(demande.dateDebut())) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être égale ou postérieure à la date de début.");
        }
    }

    private void validerFiltres(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La fin de période doit être égale ou postérieure au début.");
        }
    }

    private void verifierAccesEquipeManager(Integer idUtilisateur, Integer idEmploye) {
        if (!planningTravailRepository.estEmployeDansEquipeManager(idUtilisateur, idEmploye)) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez consulter que les salariés de votre équipe.");
        }
    }
}

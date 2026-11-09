package com.si.rh.service;

import com.si.rh.dto.rh.horaireTravail.HoraireTravailRequest;
import com.si.rh.dto.rh.horaireTravail.HorarireAvecUsage;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.HoraireTravail;
import com.si.rh.repository.HoraireTravailRepository;
import com.si.rh.repository.PlanningTravailRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class HoraireTravailService {

    private static final String CODE_STANDARD = "STANDARD";

    private final HoraireTravailRepository horaireTravailRepository;
    private final PlanningTravailRepository planningTravailRepository;

    public HoraireTravailService(
            HoraireTravailRepository horaireTravailRepository,
            PlanningTravailRepository planningTravailRepository) {
        this.horaireTravailRepository = horaireTravailRepository;
        this.planningTravailRepository = planningTravailRepository;
    }

    @Transactional(readOnly = true)
    public List<HoraireTravail> getAll() {
        return horaireTravailRepository.findAllByOrderByLibelleAsc();
    }

    @Transactional(readOnly = true)
    public HoraireTravail getById(Integer id) {
        return horaireTravailRepository.findById(id)
            .orElseThrow(() -> new RessourceIntrouvableException("Horaire introuvable : " + id));
    }

    @Transactional
    public HoraireTravail save(HoraireTravailRequest demande) {
        valider(demande);
        String code = demande.code().trim();
        verifierCodeDisponible(code, null);

        HoraireTravail horaire = new HoraireTravail();
        appliquer(horaire, demande, code);

        return horaireTravailRepository.save(horaire);
    }

    @Transactional
    public void deleteById(Integer id) {
        HoraireTravail horaire = getById(id);
        if (CODE_STANDARD.equals(horaire.getCode())) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "L'horaire STANDARD ne peut pas être supprimé.");
        }

        long nombrePlannings = planningTravailRepository.countByHoraire_Id(id);
        if (nombrePlannings > 0) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Cet horaire est affecté à " + nombrePlannings + " plannings.");
        }

        horaireTravailRepository.delete(horaire);
    }

    @Transactional
    public HoraireTravail modifier(Integer id, HoraireTravailRequest demande) {
        HoraireTravail horaire = getById(id);
        valider(demande);
        String code = demande.code().trim();

        if (CODE_STANDARD.equals(horaire.getCode()) && !CODE_STANDARD.equals(code)) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Le code de l'horaire STANDARD ne peut pas être modifié.");
        }
        verifierCodeDisponible(code, id);
        appliquer(horaire, demande, code);

        return horaireTravailRepository.save(horaire);
    }

    @Transactional
    public HoraireTravail desactiver(Integer id) {
        HoraireTravail horaire = getById(id);
        horaire.setActif(false);
        return horaireTravailRepository.save(horaire);
    }

    @Transactional
    public HoraireTravail reactiver(Integer id) {
        HoraireTravail horaire = getById(id);
        horaire.setActif(true);
        return horaireTravailRepository.save(horaire);
    }

    @Transactional(readOnly = true)
    public List<HorarireAvecUsage> getHorarireAvecUsages() {
        return horaireTravailRepository.findAllAvecNbPlannings();
    }

    private void appliquer(HoraireTravail horaire, HoraireTravailRequest demande, String code) {
        horaire.setCode(code);
        horaire.setLibelle(demande.libelle().trim());
        horaire.setHeureDebut(demande.heureDebut());
        horaire.setHeureFin(demande.heureFin());
        horaire.setDebutPause(demande.debutPause());
        horaire.setFinPause(demande.finPause());
        horaire.setToleranceRetardMinutes(demande.toleranceRetardMinutes());
        horaire.setJoursTravail(demande.joursTravail().clone());
    }

    private void valider(HoraireTravailRequest demande) {
        if (demande == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Les données de l'horaire sont obligatoires.");
        }
        if (demande.code() == null || demande.code().isBlank()) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Le code est obligatoire.");
        }
        if (demande.libelle() == null || demande.libelle().isBlank()) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Le libellé est obligatoire.");
        }
        if (demande.heureDebut() == null || demande.heureFin() == null
                || !demande.heureFin().isAfter(demande.heureDebut())) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit être supérieure à l'heure de début.");
        }

        LocalTime debutPause = demande.debutPause();
        LocalTime finPause = demande.finPause();
        if ((debutPause == null) != (finPause == null)) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La pause doit avoir un début et une fin, ou aucun des deux.");
        }
        if (debutPause != null && (debutPause.isBefore(demande.heureDebut())
                || !finPause.isAfter(debutPause) || finPause.isAfter(demande.heureFin()))) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La pause doit être comprise dans la journée et finir après son début.");
        }
        if (demande.toleranceRetardMinutes() == null || demande.toleranceRetardMinutes() < 0) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La tolérance ne peut pas être négative.");
        }

        Short[] joursTravail = demande.joursTravail();
        if (joursTravail == null || joursTravail.length == 0
                || Arrays.stream(joursTravail).anyMatch(jour -> jour == null || jour < 1 || jour > 7)) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Au moins un jour travaillé valide (de 1 à 7) est obligatoire.");
        }
    }

    private void verifierCodeDisponible(String code, Integer idIgnore) {
        horaireTravailRepository.findByCode(code)
                .filter(existant -> !Objects.equals(existant.getId(), idIgnore))
                .ifPresent(existant -> {
                    throw new RegleMetierException(HttpStatus.CONFLICT, "Ce code existe déjà");
                });
    }
}

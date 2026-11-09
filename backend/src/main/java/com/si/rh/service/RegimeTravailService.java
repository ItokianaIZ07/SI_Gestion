package com.si.rh.service;

import com.si.rh.dto.rh.regimeTravail.RegimeAvecUsage;
import com.si.rh.dto.rh.regimeTravail.RegimeTravailRequest;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.RegimeTravail;
import com.si.rh.repository.PlanningTravailRepository;
import com.si.rh.repository.RegimeTravailRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RegimeTravailService {

    private static final String CODE_TEMPS_PLEIN = "TEMPS_PLEIN";

    private final RegimeTravailRepository regimeTravailRepository;
    private final PlanningTravailRepository planningTravailRepository;

    public RegimeTravailService(
            RegimeTravailRepository regimeTravailRepository,
            PlanningTravailRepository planningTravailRepository) {
        this.regimeTravailRepository = regimeTravailRepository;
        this.planningTravailRepository = planningTravailRepository;
    }

    @Transactional(readOnly = true)
    public List<RegimeTravail> getAll() {
        return regimeTravailRepository.findAllByOrderByLibelleAsc();
    }

    @Transactional(readOnly = true)
    public RegimeTravail getById(Integer id) {
        return regimeTravailRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Régime introuvable : " + id));
    }

    public RegimeTravail creer(RegimeTravailRequest demande) {
        String code = demande.code().trim();
        verifierCodeDisponible(code, null);

        RegimeTravail regime = new RegimeTravail();
        appliquer(regime, demande, code);
        return regimeTravailRepository.save(regime);
    }

    public RegimeTravail modifier(Integer id, RegimeTravailRequest demande) {
        RegimeTravail regime = getById(id);
        String code = demande.code().trim();

        if (CODE_TEMPS_PLEIN.equals(regime.getCode()) && !CODE_TEMPS_PLEIN.equals(code)) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le code du régime TEMPS_PLEIN ne peut pas être modifié.");
        }

        verifierCodeDisponible(code, id);
        appliquer(regime, demande, code);
        return regimeTravailRepository.save(regime);
    }

    public void supprimer(Integer id) {
        RegimeTravail regime = getById(id);
        if (CODE_TEMPS_PLEIN.equals(regime.getCode())) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Le régime TEMPS_PLEIN ne peut pas être supprimé.");
        }

        long nombrePlannings = planningTravailRepository.countByRegime_Id(id);
        if (nombrePlannings > 0) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Ce régime est affecté à " + nombrePlannings + " plannings");
        }

        regimeTravailRepository.delete(regime);
    }

    private void verifierCodeDisponible(String code, Integer idIgnore) {
        regimeTravailRepository.findByCode(code)
                .filter(existant -> !existant.getId().equals(idIgnore))
                .ifPresent(existant -> {
                    throw new RegleMetierException(HttpStatus.CONFLICT, "Ce code existe déjà");
                });
    }

    private void appliquer(RegimeTravail regime, RegimeTravailRequest demande, String code) {
        regime.setCode(code);
        regime.setLibelle(demande.libelle().trim());
        regime.setDescription(demande.description());
        regime.setHeuresHebdomadaires(demande.heuresHebdomadaires());
    }

    @Transactional(readOnly = true)
    public List<RegimeAvecUsage> getAllAvecNbPlannings() {
        return regimeTravailRepository.findAllAvecNbPlannings();
    }
}

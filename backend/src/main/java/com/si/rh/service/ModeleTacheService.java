package com.si.rh.service;

import com.si.rh.dto.onboarding.ModeleTacheDTO;
import com.si.rh.dto.onboarding.ModeleTacheRequest;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.ModeleTacheOnboarding;
import com.si.rh.repository.ModeleTacheOnboardingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Modèles de tâches copiés dans l'onboarding de chaque nouvel employé (à l'embauche).
 * Modifier un modèle n'a aucun effet sur les onboardings déjà créés.
 */
@Service
@Transactional
public class ModeleTacheService {
    private final ModeleTacheOnboardingRepository repo;

    public ModeleTacheService(ModeleTacheOnboardingRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ModeleTacheDTO> tous() {
        return repo.findAll(Sort.by("categorie", "delaiJours", "id")).stream().map(this::dto).toList();
    }

    public ModeleTacheDTO creer(ModeleTacheRequest r) {
        String code = r.code().trim().toUpperCase();
        if (repo.findByCode(code).isPresent())
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce code de modèle existe déjà : " + code);
        ModeleTacheOnboarding m = new ModeleTacheOnboarding();
        appliquer(m, r, code);
        return dto(repo.save(m));
    }

    public ModeleTacheDTO modifier(Integer id, ModeleTacheRequest r) {
        ModeleTacheOnboarding m = repo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Modèle introuvable : " + id));
        String code = r.code().trim().toUpperCase();
        if (repo.findByCode(code).filter(autre -> !autre.getId().equals(id)).isPresent())
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce code de modèle existe déjà : " + code);
        appliquer(m, r, code);
        return dto(repo.save(m));
    }

    private void appliquer(ModeleTacheOnboarding m, ModeleTacheRequest r, String code) {
        m.setCode(code);
        m.setLibelle(r.libelle().trim());
        m.setCategorie(r.categorie());
        m.setDelaiJours(r.delaiJours());
        m.setActif(r.actif() == null ? Boolean.TRUE : r.actif());
    }

    private ModeleTacheDTO dto(ModeleTacheOnboarding m) {
        return new ModeleTacheDTO(m.getId(), m.getCode(), m.getLibelle(), m.getCategorie(), m.getDelaiJours(),
                m.getActif());
    }
}

package com.si.rh.service;

import com.si.rh.dto.candidat.*;
import com.si.rh.exception.*;
import com.si.rh.model.Candidat;
import com.si.rh.repository.CandidatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CandidatService {
    private final CandidatRepository repo;

    public CandidatService(CandidatRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CandidatDTO> tous() {
        return repo.findAll().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public CandidatDTO parId(Integer id) {
        return dto(trouver(id));
    }

    public CandidatDTO creer(CandidatEcritureDTO d) {
        if (repo.existsByEmailIgnoreCase(d.email()))
            throw new RegleMetierException("Cette adresse e-mail est déjà utilisée.");
        if (d.numeroCin() != null && !d.numeroCin().isBlank() && repo.existsByNumeroCin(d.numeroCin()))
            throw new RegleMetierException("Ce numéro CIN est déjà utilisé.");
        Candidat c = new Candidat();
        appliquer(c, d);
        return dto(repo.save(c));
    }

    public CandidatDTO modifier(Integer id, CandidatEcritureDTO d) {
        Candidat c = trouver(id);
        if (repo.existsByEmailIgnoreCase(d.email()) && !c.getEmail().equalsIgnoreCase(d.email()))
            throw new RegleMetierException("Cette adresse e-mail est déjà utilisée.");
        if (d.numeroCin() != null && !d.numeroCin().isBlank() && repo.existsByNumeroCin(d.numeroCin())
                && !d.numeroCin().equals(c.getNumeroCin()))
            throw new RegleMetierException("Ce numéro CIN est déjà utilisé.");
        appliquer(c, d);
        return dto(repo.save(c));
    }

    public void supprimer(Integer id) {
        repo.delete(trouver(id));
    }

    private Candidat trouver(Integer id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Candidat introuvable : " + id));
    }

    private void appliquer(Candidat c, CandidatEcritureDTO d) {
        c.setNom(d.nom());
        c.setPrenom(d.prenom());
        c.setEmail(d.email());
        c.setTelephone(d.telephone());
        c.setNumeroCin(d.numeroCin());
        c.setExperience(d.experience());
    }

    private CandidatDTO dto(Candidat c) {
        return new CandidatDTO(c.getId(), c.getNom(), c.getPrenom(), c.getEmail(), c.getTelephone(), c.getNumeroCin(),
                c.getExperience(), c.getDateCreation());
    }
}

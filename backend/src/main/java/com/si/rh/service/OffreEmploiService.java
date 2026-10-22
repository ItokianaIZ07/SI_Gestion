package com.si.rh.service;

import com.si.rh.dto.offre.*;
import com.si.rh.exception.*;
import com.si.rh.model.*;
import com.si.rh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class OffreEmploiService {
    private final OffreEmploiRepository repo;
    private final RecrutementRepository recrutementRepo;

    public OffreEmploiService(OffreEmploiRepository repo, RecrutementRepository recrutementRepo) {
        this.repo = repo;
        this.recrutementRepo = recrutementRepo;
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiDTO> tous() {
        return repo.findAll().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public OffreEmploiDTO parId(Integer id) {
        return dto(trouver(id));
    }

    public OffreEmploiDTO creer(OffreEmploiEcritureDTO d) {
        if (repo.existsByRecrutement_Id(d.recrutementId()))
            throw new RegleMetierException("Une offre existe déjà pour ce recrutement.");
        OffreEmploi o = new OffreEmploi();
        appliquer(o, d);
        return dto(repo.save(o));
    }

    public OffreEmploiDTO modifier(Integer id, OffreEmploiEcritureDTO d) {
        OffreEmploi o = trouver(id);
        if (repo.existsByRecrutement_Id(d.recrutementId()) && !o.getRecrutement().getId().equals(d.recrutementId()))
            throw new RegleMetierException("Une offre existe déjà pour ce recrutement.");
        appliquer(o, d);
        return dto(repo.save(o));
    }

    public void supprimer(Integer id) {
        repo.delete(trouver(id));
    }

    private void appliquer(OffreEmploi o, OffreEmploiEcritureDTO d) {
        Recrutement r = recrutementRepo.findById(d.recrutementId())
                .orElseThrow(() -> new RessourceIntrouvableException("Recrutement introuvable : " + d.recrutementId()));
        if (d.salaireMin() != null && d.salaireMax() != null && d.salaireMin().compareTo(d.salaireMax()) > 0)
            throw new RegleMetierException("Le salaire minimum ne peut pas dépasser le salaire maximum.");
        o.setRecrutement(r);
        o.setTitre(d.titre());
        o.setDescriptionPoste(d.descriptionPoste());
        o.setMissions(d.missions());
        o.setCompetencesRequises(d.competencesRequises());
        o.setNiveauEtude(d.niveauEtude());
        o.setExperienceMinAnnees(d.experienceMinAnnees() == null ? 0 : d.experienceMinAnnees());
        o.setSalaireMin(d.salaireMin());
        o.setSalaireMax(d.salaireMax());
        if (d.statut() != null)
            o.setStatut(d.statut());
    }

    private OffreEmploi trouver(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre d'emploi introuvable : " + id));
    }

    private OffreEmploiDTO dto(OffreEmploi o) {
        return new OffreEmploiDTO(o.getId(), o.getRecrutement().getId(), o.getTitre(), o.getDescriptionPoste(),
                o.getMissions(), o.getCompetencesRequises(), o.getNiveauEtude(), o.getExperienceMinAnnees(),
                o.getSalaireMin(), o.getSalaireMax(), o.getDateCreation(), o.getStatut());
    }
}

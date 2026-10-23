package com.si.rh.service;

import com.si.rh.dto.publication.*;
import com.si.rh.exception.*;
import com.si.rh.model.*;
import com.si.rh.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PublicationOffreService {
    private final PublicationOffreRepository repo;
    private final OffreEmploiRepository offreRepo;

    public PublicationOffreService(PublicationOffreRepository repo, OffreEmploiRepository offreRepo) {
        this.repo = repo;
        this.offreRepo = offreRepo;
    }

    @Transactional(readOnly = true)
    public List<PublicationOffreDTO> tous() {
        return repo.findAll().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public PublicationOffreDTO parId(Integer id) {
        return dto(trouver(id));
    }

    public PublicationOffreDTO creer(PublicationOffreEcritureDTO d) {
        PublicationOffre p = new PublicationOffre();
        appliquer(p, d);
        return dto(repo.save(p));
    }

    public PublicationOffreDTO modifier(Integer id, PublicationOffreEcritureDTO d) {
        PublicationOffre p = trouver(id);
        appliquer(p, d);
        return dto(repo.save(p));
    }

    public void supprimer(Integer id) {
        repo.delete(trouver(id));
    }

    private void appliquer(PublicationOffre p, PublicationOffreEcritureDTO d) {
        OffreEmploi o = offreRepo.findById(d.offreId())
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable : " + d.offreId()));
        LocalDate date = d.datePublication() == null ? LocalDate.now() : d.datePublication();
        if (d.dateExpiration() != null && d.dateExpiration().isBefore(date))
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "La date d'expiration ne peut pas précéder la date de publication.");
        p.setOffre(o);
        p.setPlateforme(d.plateforme());
        p.setUrlPublication(d.urlPublication());
        p.setDatePublication(date);
        p.setDateExpiration(d.dateExpiration());
        if (d.statut() != null)
            p.setStatut(d.statut());
    }

    private PublicationOffre trouver(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Publication introuvable : " + id));
    }

    private PublicationOffreDTO dto(PublicationOffre p) {
        return new PublicationOffreDTO(p.getId(), p.getOffre().getId(), p.getPlateforme(), p.getUrlPublication(),
                p.getDatePublication(), p.getDateExpiration(), p.getStatut());
    }
}

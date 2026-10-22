package com.si.rh.service;

import com.si.rh.dto.offre.OffreEmploiDTO;
import com.si.rh.dto.publication.PublicationOffreDTO;
import com.si.rh.model.OffreEmploi;
import com.si.rh.model.PublicationOffre;
import com.si.rh.model.enums.StatutOffre;
import com.si.rh.model.enums.StatutPublication;
import com.si.rh.repository.PublicationOffreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/** Lecture publique : uniquement ce qui est réellement visible par un candidat. */
@Service
@Transactional(readOnly = true)
public class ClientService {
    private final PublicationOffreRepository publicationRepo;

    public ClientService(PublicationOffreRepository publicationRepo) {
        this.publicationRepo = publicationRepo;
    }

    public List<PublicationOffreDTO> publicationsActives() {
        return actives().stream().map(this::dto).toList();
    }

    public List<OffreEmploiDTO> offresPubliees() {
        return actives().stream().map(PublicationOffre::getOffre).distinct().map(this::dto).toList();
    }

    private List<PublicationOffre> actives() {
        LocalDate jour = LocalDate.now();
        return publicationRepo.findByStatut(StatutPublication.PUBLIEE).stream()
                .filter(p -> p.getOffre().getStatut() == StatutOffre.VALIDEE)
                .filter(p -> !p.getDatePublication().isAfter(jour))
                .filter(p -> p.getDateExpiration() == null || !p.getDateExpiration().isBefore(jour))
                .toList();
    }

    private PublicationOffreDTO dto(PublicationOffre p) {
        return new PublicationOffreDTO(p.getId(), p.getOffre().getId(), p.getPlateforme(), p.getUrlPublication(),
                p.getDatePublication(), p.getDateExpiration(), p.getStatut());
    }

    private OffreEmploiDTO dto(OffreEmploi o) {
        return new OffreEmploiDTO(o.getId(), o.getRecrutement().getId(), o.getTitre(), o.getDescriptionPoste(),
                o.getMissions(), o.getCompetencesRequises(), o.getNiveauEtude(), o.getExperienceMinAnnees(),
                o.getSalaireMin(), o.getSalaireMax(), o.getDateCreation(), o.getStatut());
    }
}

package com.si.rh.service;

import com.si.rh.dto.candidature.*;
import com.si.rh.exception.*;
import com.si.rh.model.*;
import com.si.rh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CandidatureService {
    private final CandidatureRepository repo;
    private final CandidatRepository candidatRepo;
    private final RecrutementRepository recrutementRepo;
    private final CvRepository cvRepo;
    private final PublicationOffreRepository publicationRepo;

    public CandidatureService(CandidatureRepository repo, CandidatRepository candidatRepo,
            RecrutementRepository recrutementRepo, CvRepository cvRepo, PublicationOffreRepository publicationRepo) {
        this.repo = repo;
        this.candidatRepo = candidatRepo;
        this.recrutementRepo = recrutementRepo;
        this.cvRepo = cvRepo;
        this.publicationRepo = publicationRepo;
    }

    @Transactional(readOnly = true)
    public List<CandidatureDTO> tous() {
        return repo.findAll().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public CandidatureDTO parId(Integer id) {
        return dto(trouver(id));
    }

    public CandidatureDTO creer(CandidatureEcritureDTO d) {
        if (repo.existsByCandidat_IdAndRecrutement_Id(d.candidatId(), d.recrutementId()))
            throw new RegleMetierException("Ce candidat a déjà postulé à ce recrutement.");
        Candidature c = new Candidature();
        appliquer(c, d);
        return dto(repo.save(c));
    }

    public CandidatureDTO modifier(Integer id, CandidatureEcritureDTO d) {
        Candidature c = trouver(id);
        if (repo.existsByCandidat_IdAndRecrutement_Id(d.candidatId(), d.recrutementId())
                && (!c.getCandidat().getId().equals(d.candidatId())
                        || !c.getRecrutement().getId().equals(d.recrutementId())))
            throw new RegleMetierException("Ce candidat a déjà postulé à ce recrutement.");
        appliquer(c, d);
        return dto(repo.save(c));
    }

    public void supprimer(Integer id) {
        repo.delete(trouver(id));
    }

    private void appliquer(Candidature c, CandidatureEcritureDTO d) {
        Candidat candidat = candidatRepo.findById(d.candidatId())
                .orElseThrow(() -> new RessourceIntrouvableException("Candidat introuvable : " + d.candidatId()));
        Recrutement recrutement = recrutementRepo.findById(d.recrutementId())
                .orElseThrow(() -> new RessourceIntrouvableException("Recrutement introuvable : " + d.recrutementId()));
        Cv cv = cvRepo.findById(d.cvId())
                .orElseThrow(() -> new RessourceIntrouvableException("CV introuvable : " + d.cvId()));
        if (!cv.getCandidat().getId().equals(candidat.getId()))
            throw new RegleMetierException("Le CV fourni n'appartient pas à ce candidat.");
        PublicationOffre publication = null;
        if (d.publicationId() != null) {
            publication = publicationRepo.findById(d.publicationId()).orElseThrow(
                    () -> new RessourceIntrouvableException("Publication introuvable : " + d.publicationId()));
            if (!publication.getOffre().getRecrutement().getId().equals(recrutement.getId()))
                throw new RegleMetierException("La publication sélectionnée ne correspond pas à ce recrutement.");
        }
        c.setCandidat(candidat);
        c.setRecrutement(recrutement);
        c.setCv(cv);
        c.setPublication(publication);
        if (d.statut() != null)
            c.setStatut(d.statut());
        c.setScore(d.score());
        c.setRemarque(d.remarque());
    }

    private Candidature trouver(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature introuvable : " + id));
    }

    private CandidatureDTO dto(Candidature c) {
        return new CandidatureDTO(c.getId(), c.getCandidat().getId(), c.getRecrutement().getId(), c.getCv().getId(),
                c.getPublication() == null ? null : c.getPublication().getId(), c.getDateCandidature(), c.getStatut(),
                c.getScore(), c.getRemarque());
    }
}

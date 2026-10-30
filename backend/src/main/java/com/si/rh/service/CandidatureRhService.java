package com.si.rh.service;

import com.si.rh.dto.rh.CandidatureRhDTO;
import com.si.rh.dto.rh.CandidatureStatutDTO;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Candidature;
import com.si.rh.model.enums.StatutCandidature;
import com.si.rh.model.enums.StatutLecture;
import com.si.rh.repository.CandidatureRepository;
import com.si.rh.repository.CvCandidatureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CandidatureRhService {
    private final CvCandidatureRepository lignesRepo;
    private final CandidatureRepository candidatureRepo;

    public CandidatureRhService(CvCandidatureRepository lignesRepo, CandidatureRepository candidatureRepo) {
        this.lignesRepo = lignesRepo;
        this.candidatureRepo = candidatureRepo;
    }

    @Transactional(readOnly = true)
    public List<CandidatureRhDTO> liste(Integer recrutementId) {
        return recrutementId == null ? lignesRepo.toutes() : lignesRepo.parRecrutement(recrutementId);
    }

    public CandidatureStatutDTO preselectionner(Integer id, String remarque) {
        return changer(id, StatutCandidature.PRESELECTIONNE, remarque);
    }

    public CandidatureStatutDTO rejeter(Integer id, String remarque) {
        return changer(id, StatutCandidature.REJETE, remarque);
    }

    public void marquerCvLu(Integer cvId) {
        if (lignesRepo.changerStatutLecture(cvId, StatutLecture.LU) == 0)
            throw new RessourceIntrouvableException("CV introuvable : " + cvId);
    }

    /** Seule une candidature EN_ATTENTE peut être présélectionnée ou rejetée. */
    private CandidatureStatutDTO changer(Integer id, StatutCandidature cible, String remarque) {
        Candidature c = candidatureRepo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature introuvable : " + id));
        if (c.getStatut() != StatutCandidature.EN_ATTENTE)
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Cette candidature a déjà été traitée (statut actuel : " + c.getStatut() + ").");
        c.setStatut(cible);
        if (remarque != null && !remarque.isBlank())
            c.setRemarque(remarque.trim());
        candidatureRepo.save(c);
        return new CandidatureStatutDTO(c.getId(), c.getStatut(), c.getRemarque());
    }
}

package com.si.rh.service;

import com.si.rh.dto.cv.*;
import com.si.rh.exception.*;
import com.si.rh.model.*;
import com.si.rh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class CvService {
    private final CvRepository cvRepo;
    private final CandidatRepository candidatRepo;

    public CvService(CvRepository cvRepo, CandidatRepository candidatRepo) {
        this.cvRepo = cvRepo;
        this.candidatRepo = candidatRepo;
    }

    @Transactional(readOnly = true)
    public List<CvDTO> tous() {
        return cvRepo.findAll().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public CvDTO parId(Integer id) {
        return dto(trouver(id));
    }

    public CvDTO creer(CvEcritureDTO d, byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("Le fichier CV est vide.");
        Candidat candidat = candidatRepo.findById(d.candidatId())
                .orElseThrow(() -> new RessourceIntrouvableException("Candidat introuvable : " + d.candidatId()));
        Cv c = new Cv();
        c.setCandidat(candidat);
        c.setDetails(d.details());
        c.setNomFichier(d.nomFichier());
        c.setMimeType(d.mimeType());
        c.setBytes(bytes);
        return dto(cvRepo.save(c));
    }

    public CvDTO modifier(Integer id, CvEcritureDTO d, byte[] bytes) {
        Cv c = trouver(id);
        Candidat candidat = candidatRepo.findById(d.candidatId())
                .orElseThrow(() -> new RessourceIntrouvableException("Candidat introuvable : " + d.candidatId()));
        c.setCandidat(candidat);
        c.setDetails(d.details());
        c.setNomFichier(d.nomFichier());
        c.setMimeType(d.mimeType());
        if (bytes != null && bytes.length > 0)
            c.setBytes(bytes);
        return dto(cvRepo.save(c));
    }

    public void supprimer(Integer id) {
        cvRepo.delete(trouver(id));
    }

    @Transactional(readOnly = true)
    public Cv fichier(Integer id) {
        return trouver(id);
    }

    private Cv trouver(Integer id) {
        return cvRepo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("CV introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<CvDTO> parCandidat(Integer candidatId) {
        if (!candidatRepo.existsById(candidatId)) {
            throw new RessourceIntrouvableException(
                    "Candidat introuvable : " + candidatId
            );
        }

        return cvRepo.findByCandidatId(candidatId)
                .stream()
                .map(this::dto)
                .toList();
    }

    private CvDTO dto(Cv c) {
        return new CvDTO(c.getId(), c.getCandidat().getId(), c.getDetails(), c.getNomFichier(), c.getMimeType(),
                c.getDateDepot(), c.getStatutLecture());
    }
}

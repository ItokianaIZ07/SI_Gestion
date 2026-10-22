package com.si.rh.service;

import com.si.rh.dto.besoin.BesoinDTO;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Besoin;
import com.si.rh.repository.BesoinRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BesoinService {
    private final BesoinRepository repo;

    public BesoinService(BesoinRepository repo) {
        this.repo = repo;
    }

    public List<Besoin> getAll(){
        return repo.findAll();
    }

    public List<BesoinDTO> tous() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "dateDemande")).stream().map(this::dto).toList();
    }

    public BesoinDTO parId(Integer id) {
        return dto(repo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Besoin introuvable : " + id)));
    }

    private BesoinDTO dto(Besoin b) {
        return new BesoinDTO(b.getId(), b.getTypeBesoin().getLibelle(), b.getPoste().getNom(),
                b.getPoste().getDepartement().getNom(), b.getEffectif(), b.getPriorite(),
                b.getDateArriveeSouhaitee(), b.getLieuTravail(), b.getMotif(), b.getExperienceRequise(),
                b.getNiveauEtude(), b.getCompetencesRequises(), b.getSalaireEstime(), b.getStatutGlobal(),
                b.getDateDemande());
    }
}

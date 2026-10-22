package com.si.rh.service;

import com.si.rh.dto.recrutement.*;
import com.si.rh.exception.*;
import com.si.rh.model.*;
import com.si.rh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class RecrutementService {
    private final RecrutementRepository repo;
    private final BesoinRepository besoinRepo;
    private final UtilisateurRepository utilisateurRepo;

    public RecrutementService(RecrutementRepository repo, BesoinRepository besoinRepo,
            UtilisateurRepository utilisateurRepo) {
        this.repo = repo;
        this.besoinRepo = besoinRepo;
        this.utilisateurRepo = utilisateurRepo;
    }

    @Transactional(readOnly = true)
    public List<RecrutementDTO> tous() {
        return repo.findAll().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public RecrutementDTO parId(Integer id) {
        return dto(trouver(id));
    }

    public RecrutementDTO creer(RecrutementEcritureDTO d) {
        if (repo.existsByReference(d.reference()))
            throw new RegleMetierException("Cette référence de recrutement existe déjà.");
        if (repo.existsByBesoin_Id(d.besoinId()))
            throw new RegleMetierException("Ce besoin est déjà associé à un recrutement.");
        Recrutement r = new Recrutement();
        appliquer(r, d);
        return dto(repo.save(r));
    }

    public RecrutementDTO modifier(Integer id, RecrutementEcritureDTO d) {
        Recrutement r = trouver(id);
        if (repo.existsByReference(d.reference()) && !r.getReference().equals(d.reference()))
            throw new RegleMetierException("Cette référence de recrutement existe déjà.");
        if (repo.existsByBesoin_Id(d.besoinId()) && !r.getBesoin().getId().equals(d.besoinId()))
            throw new RegleMetierException("Ce besoin est déjà associé à un recrutement.");
        appliquer(r, d);
        return dto(repo.save(r));
    }

    public void supprimer(Integer id) {
        repo.delete(trouver(id));
    }

    private void appliquer(Recrutement r, RecrutementEcritureDTO d) {
        Besoin b = besoinRepo.findById(d.besoinId())
                .orElseThrow(() -> new RessourceIntrouvableException("Besoin introuvable : " + d.besoinId()));
        r.setBesoin(b);
        r.setReference(d.reference());
        r.setTitre(d.titre());
        r.setDescription(d.description());
        r.setDateOuverture(d.dateOuverture() == null ? LocalDate.now() : d.dateOuverture());
        r.setDateCloture(d.dateCloture());
        if (d.statut() != null)
            r.setStatut(d.statut());
        if (d.responsableId() == null)
            r.setResponsable(null);
        else
            r.setResponsable(utilisateurRepo.findById(d.responsableId()).orElseThrow(
                    () -> new RessourceIntrouvableException("Responsable introuvable : " + d.responsableId())));
        if (r.getDateCloture() != null && r.getDateCloture().isBefore(r.getDateOuverture()))
            throw new RegleMetierException("La date de clôture ne peut pas précéder la date d'ouverture.");
    }

    private Recrutement trouver(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Recrutement introuvable : " + id));
    }

    private RecrutementDTO dto(Recrutement r) {
        return new RecrutementDTO(r.getId(), r.getBesoin().getId(), r.getReference(), r.getTitre(), r.getDescription(),
                r.getDateOuverture(), r.getDateCloture(), r.getStatut(),
                r.getResponsable() == null ? null : r.getResponsable().getId(), r.getDateCreation(),
                r.getDateModification());
    }
}

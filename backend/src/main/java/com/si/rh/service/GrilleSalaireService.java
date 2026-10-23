package com.si.rh.service;

// BY : Fenosoa

import com.si.rh.dto.GrilleSalaireRequest;
import com.si.rh.dto.GrilleSalaireResponse;
import com.si.rh.dto.PosteResponse;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.GrilleSalaire;
import com.si.rh.model.Poste;
import com.si.rh.repository.GrilleSalaireRepository;
import com.si.rh.repository.PosteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GrilleSalaireService {

    private final GrilleSalaireRepository grilleRepository;
    private final PosteRepository posteRepository;

    public GrilleSalaireService(GrilleSalaireRepository grilleRepository,
                                PosteRepository posteRepository) {
        this.grilleRepository = grilleRepository;
        this.posteRepository = posteRepository;
    }

    @Transactional(readOnly = true)
    public List<GrilleSalaireResponse> listerGrilles() {
        return grilleRepository.findAllAvecPoste().stream()
                .map(this::versReponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PosteResponse> listerPostes() {
        return posteRepository.findActifsAvecDepartement().stream()
                .map(p -> new PosteResponse(p.getId(), p.getNom(), p.getDepartement().getNom()))
                .toList();
    }

    @Transactional
    public GrilleSalaireResponse creerGrille(GrilleSalaireRequest demande) {
        Poste poste = posteRepository.findById(demande.idPoste())
                .orElseThrow(() -> new RegleMetierException(HttpStatus.NOT_FOUND, "Poste introuvable."));

        GrilleSalaire grille = new GrilleSalaire();
        grille.setPoste(poste);
        appliquer(grille, demande, null);
        return versReponse(grilleRepository.saveAndFlush(grille));
    }

    @Transactional
    public GrilleSalaireResponse modifierGrille(Integer id, GrilleSalaireRequest demande) {
        GrilleSalaire grille = grilleRepository.findById(id)
                .orElseThrow(() -> new RegleMetierException(HttpStatus.NOT_FOUND, "Grille introuvable."));

        if (!grille.getPoste().getId().equals(demande.idPoste())) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le poste d'une grille ne peut pas être modifié.");
        }
        appliquer(grille, demande, id);
        return versReponse(grilleRepository.saveAndFlush(grille));
    }

    // ----- Règles métier -----

    /** Vérifie toutes les règles puis recopie les valeurs (rien n'est modifié si une règle échoue). */
    private void appliquer(GrilleSalaire grille, GrilleSalaireRequest demande, Integer idIgnore) {
        if (demande.valeurMax().compareTo(demande.valeurMin()) < 0) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le salaire maximum doit être supérieur ou égal au minimum.");
        }
        if (demande.dateFinValidite() != null
                && demande.dateFinValidite().isBefore(demande.dateDebutValidite())) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit suivre la date de début.");
        }

        PeriodeValidite periode = new PeriodeValidite(demande.dateDebutValidite(), demande.dateFinValidite());
        verifierAucunChevauchement(grille.getPoste().getId(), periode, idIgnore);

        grille.setValeurMin(demande.valeurMin());
        grille.setValeurMax(demande.valeurMax());
        grille.setDateDebutValidite(demande.dateDebutValidite());
        grille.setDateFinValidite(demande.dateFinValidite());
    }

    private void verifierAucunChevauchement(Integer idPoste, PeriodeValidite periode, Integer idIgnore) {
        for (GrilleSalaire existante : grilleRepository.findByPosteId(idPoste)) {
            if (existante.getId().equals(idIgnore)) {
                continue;   // on ne compare pas la grille avec elle-même lors d'une modification
            }
            PeriodeValidite autre = new PeriodeValidite(
                    existante.getDateDebutValidite(), existante.getDateFinValidite());
            if (periode.seChevauche(autre)) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                        "Cette période chevauche une grille existante pour ce poste (" + autre + ").");
            }
        }
    }

    private GrilleSalaireResponse versReponse(GrilleSalaire g) {
        return new GrilleSalaireResponse(
                g.getId(),
                g.getPoste().getId(),
                g.getPoste().getNom(),
                g.getPoste().getDepartement().getNom(),
                g.getValeurMin(),
                g.getValeurMax(),
                g.getDateDebutValidite(),
                g.getDateFinValidite());
    }
}
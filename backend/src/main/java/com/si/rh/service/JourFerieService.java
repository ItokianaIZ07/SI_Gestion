package com.si.rh.service;

import com.si.rh.dto.rh.jourFerie.JourFerieAvecJourSemaine;
import com.si.rh.dto.rh.jourFerie.JourFerieRequest;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.JourFerie;
import com.si.rh.repository.JourFerieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JourFerieService {

    private final JourFerieRepository jourFerieRepository;

    public JourFerieService(JourFerieRepository jourFerieRepository) {
        this.jourFerieRepository = jourFerieRepository;
    }

    @Transactional(readOnly = true)
    public List<JourFerie> getAll() {
        return jourFerieRepository.findAllByOrderByDateJourAsc();
    }

    @Transactional(readOnly = true)
    public Optional<JourFerie> getByDate(LocalDate dateJour) {
        return jourFerieRepository.findById(dateJour);
    }

    @Transactional(readOnly = true)
    public List<JourFerieAvecJourSemaine> listerParAnnee(Integer annee) {
        if (annee == null || annee < 1) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "L'année doit être valide.");
        }
        return jourFerieRepository.findByAnnee(annee);
    }

    @Transactional(readOnly = true)
    public List<Integer> listerAnneesDisponibles() {
        return jourFerieRepository.findAllByOrderByDateJourAsc().stream()
                .map(jourFerie -> jourFerie.getDateJour().getYear())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean estFerie(LocalDate dateJour) {
        validerDate(dateJour);
        return jourFerieRepository.existsById(dateJour);
    }

    public JourFerie ajouter(JourFerieRequest demande) {
        valider(demande);
        LocalDate dateJour = demande.dateJour();
        if (jourFerieRepository.existsById(dateJour)) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce jour est déjà férié");
        }
        return jourFerieRepository.save(new JourFerie(dateJour, demande.libelle().trim()));
    }

    public JourFerie modifier(LocalDate dateJourActuelle, JourFerieRequest demande) {
        valider(demande);
        LocalDate nouvelleDate = demande.dateJour();
        String libelle = demande.libelle();
        JourFerie jourFerie = jourFerieRepository.findById(dateJourActuelle)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun jour férié trouvé à la date " + dateJourActuelle + "."));

        if (!dateJourActuelle.equals(nouvelleDate)) {
            if (jourFerieRepository.existsById(nouvelleDate)) {
                throw new RegleMetierException(HttpStatus.CONFLICT, "Ce jour est déjà férié");
            }
            jourFerieRepository.delete(jourFerie);
            jourFerieRepository.flush();
            jourFerie = new JourFerie(nouvelleDate, libelle.trim());
            return jourFerieRepository.save(jourFerie);
        }

        jourFerie.setLibelle(libelle.trim());
        return jourFerieRepository.save(jourFerie);
    }

    public void supprimer(LocalDate dateJour) {
        validerDate(dateJour);
        JourFerie jourFerie = jourFerieRepository.findById(dateJour)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun jour férié trouvé à la date " + dateJour + "."));
        jourFerieRepository.delete(jourFerie);
    }

    private void valider(JourFerieRequest demande) {
        if (demande == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Les données du jour férié sont obligatoires.");
        }
        validerDate(demande.dateJour());
        if (demande.libelle() == null || demande.libelle().isBlank()) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Le libellé est obligatoire.");
        }
        if (demande.libelle().trim().length() > 100) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "Le libellé ne peut pas dépasser 100 caractères.");
        }
    }

    private void validerDate(LocalDate dateJour) {
        if (dateJour == null) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "La date est obligatoire et doit être valide.");
        }
    }
}

package com.si.rh.service;

import com.si.rh.dto.depot.DepotCvDTO;
import com.si.rh.dto.depot.DepotCvResultatDTO;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.*;
import com.si.rh.model.enums.StatutOffre;
import com.si.rh.model.enums.StatutPublication;
import com.si.rh.model.enums.StatutRecrutement;
import com.si.rh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Dépôt public d'un CV : candidat + CV + candidature en une seule transaction.
 * Toute exception annule l'ensemble (aucun candidat orphelin).
 */
@Service
@Transactional
public class DepotCvService {
    private static final int TAILLE_MAX = 5 * 1024 * 1024;

    private final CandidatRepository candidatRepo;
    private final CvRepository cvRepo;
    private final CandidatureRepository candidatureRepo;
    private final RecrutementRepository recrutementRepo;
    private final PublicationOffreRepository publicationRepo;

    public DepotCvService(CandidatRepository candidatRepo, CvRepository cvRepo, CandidatureRepository candidatureRepo,
            RecrutementRepository recrutementRepo, PublicationOffreRepository publicationRepo) {
        this.candidatRepo = candidatRepo;
        this.cvRepo = cvRepo;
        this.candidatureRepo = candidatureRepo;
        this.recrutementRepo = recrutementRepo;
        this.publicationRepo = publicationRepo;
    }

    public DepotCvResultatDTO deposer(DepotCvDTO d, byte[] bytes) {
        verifierFichier(bytes);

        PublicationOffre publication = null;
        Recrutement recrutement = null;
        LocalDate jour = LocalDate.now();

        if (d.publicationId() != null) {
            publication = publicationRepo.findById(d.publicationId()).orElseThrow(
                    () -> new RessourceIntrouvableException("Publication introuvable : " + d.publicationId()));
            boolean active = publication.getStatut() == StatutPublication.PUBLIEE
                    && publication.getOffre().getStatut() == StatutOffre.VALIDEE
                    && (publication.getDateExpiration() == null || !publication.getDateExpiration().isBefore(jour));
            if (!active)
                throw new RegleMetierException("Cette offre n'est plus ouverte aux candidatures.");
            recrutement = publication.getOffre().getRecrutement();
            if (d.recrutementId() != null && !d.recrutementId().equals(recrutement.getId()))
                throw new RegleMetierException("La publication sélectionnée ne correspond pas à ce recrutement.");
        } else if (d.recrutementId() != null) {
            recrutement = recrutementRepo.findById(d.recrutementId()).orElseThrow(
                    () -> new RessourceIntrouvableException("Recrutement introuvable : " + d.recrutementId()));
        }

        if (recrutement != null) {
            StatutRecrutement s = recrutement.getStatut();
            if (s == StatutRecrutement.TERMINE || s == StatutRecrutement.ANNULE || s == StatutRecrutement.SUSPENDU)
                throw new RegleMetierException("Ce recrutement n'accepte plus de candidatures.");
            if (recrutement.getDateCloture() != null && recrutement.getDateCloture().isBefore(jour))
                throw new RegleMetierException("La date limite de candidature est dépassée.");
        }

        Candidat candidat = trouverOuCreerCandidat(d);

        if (recrutement != null && candidat.getId() != null
                && candidatureRepo.existsByCandidat_IdAndRecrutement_Id(candidat.getId(), recrutement.getId()))
            throw new RegleMetierException("Vous avez déjà postulé à cette offre.");

        Cv cv = new Cv();
        cv.setCandidat(candidat);
        cv.setDetails(vide(d.details()) ? null : d.details().trim());
        cv.setNomFichier(nettoyerNomFichier(d.nomFichier()));
        cv.setMimeType("application/pdf");
        cv.setBytes(bytes);
        cv = cvRepo.save(cv);

        Integer candidatureId = null;
        if (recrutement != null) {
            Candidature c = new Candidature();
            c.setCandidat(candidat);
            c.setRecrutement(recrutement);
            c.setCv(cv);
            c.setPublication(publication);
            candidatureId = candidatureRepo.save(c).getId();
        }
        return new DepotCvResultatDTO(candidat.getId(), cv.getId(), candidatureId);
    }

    /**
     * Un candidat existant (même e-mail) est réutilisé tel quel, sans modification de ses données :
     * l'endpoint étant public, on ne laisse pas n'importe qui écraser un dossier. Le CIN doit concorder.
     */
    private Candidat trouverOuCreerCandidat(DepotCvDTO d) {
        String email = d.email().trim();
        String cin = vide(d.numeroCin()) ? null : d.numeroCin().trim();

        Optional<Candidat> existant = candidatRepo.findByEmailIgnoreCase(email);
        if (existant.isPresent()) {
            Candidat c = existant.get();
            if (!Objects.equals(c.getNumeroCin(), cin))
                throw new RegleMetierException("Cette adresse e-mail est déjà associée à un autre dossier candidat.");
            return c;
        }
        if (cin != null && candidatRepo.existsByNumeroCin(cin))
            throw new RegleMetierException("Ce numéro CIN est déjà utilisé.");

        Candidat c = new Candidat();
        c.setNom(d.nom().trim());
        c.setPrenom(d.prenom().trim());
        c.setEmail(email);
        c.setTelephone(vide(d.telephone()) ? null : d.telephone().trim());
        c.setNumeroCin(cin);
        c.setExperience(d.experience());
        return candidatRepo.save(c);
    }

    private void verifierFichier(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("Le fichier CV est vide.");
        if (bytes.length > TAILLE_MAX)
            throw new IllegalArgumentException("Le fichier dépasse 5 Mo.");
        String debut = new String(bytes, 0, Math.min(bytes.length, 1024), StandardCharsets.ISO_8859_1);
        if (!debut.contains("%PDF-"))
            throw new IllegalArgumentException("Le CV doit être un fichier PDF valide.");
    }

    private String nettoyerNomFichier(String nom) {
        String n = nom.substring(Math.max(nom.lastIndexOf('/'), nom.lastIndexOf('\\')) + 1)
                .replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (n.isEmpty())
            n = "cv.pdf";
        return n.length() > 255 ? n.substring(0, 255) : n;
    }

    private boolean vide(String s) {
        return s == null || s.isBlank();
    }
}

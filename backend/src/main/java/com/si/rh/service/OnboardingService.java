package com.si.rh.service;

import com.si.rh.dto.onboarding.*;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.*;
import com.si.rh.model.enums.*;
import com.si.rh.repository.ContratOnboardingRepository;
import com.si.rh.repository.DocumentEmployeRepository;
import com.si.rh.repository.OnboardingRepository;
import com.si.rh.repository.TacheOnboardingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Suivi de l'intégration d'un nouvel employé : tâches (créées à l'embauche depuis les modèles)
 * et documents administratifs. Le statut de l'onboarding est déduit de ses tâches.
 */
@Service
@Transactional
public class OnboardingService {
    private static final int TAILLE_MAX = 5 * 1024 * 1024;
    /** Code du modèle de tâche « contrat signé » : cochée automatiquement quand le contrat devient ACTIF. */
    static final String CODE_TACHE_CONTRAT = "CONTRAT_SIGNE";

    private final OnboardingRepository onboardingRepo;
    private final TacheOnboardingRepository tacheRepo;
    private final DocumentEmployeRepository documentRepo;
    private final ContratOnboardingRepository contratRepo;

    public OnboardingService(OnboardingRepository onboardingRepo, TacheOnboardingRepository tacheRepo,
            DocumentEmployeRepository documentRepo, ContratOnboardingRepository contratRepo) {
        this.onboardingRepo = onboardingRepo;
        this.tacheRepo = tacheRepo;
        this.documentRepo = documentRepo;
        this.contratRepo = contratRepo;
    }

    // ====================== Lecture ======================

    /** Coche « contrat signé » pour les employés dont le contrat est maintenant ACTIF. */
    private void synchroniserContrats() {
        for (TacheOnboarding t : tacheRepo.findContratSigneEnAttente(CODE_TACHE_CONTRAT,
                List.of(StatutTache.A_FAIRE, StatutTache.EN_COURS))) {
            t.setStatut(StatutTache.FAIT);
            t.setDateRealisation(OffsetDateTime.now());
            if (t.getRemarque() == null)
                t.setRemarque("Validée automatiquement : contrat signé des deux parties.");
            tacheRepo.save(t);
            recalculerStatut(t.getOnboarding());
        }
    }

    // Transaction en écriture (pas readOnly) : la lecture déclenche la synchronisation du contrat.
    public List<OnboardingResumeDTO> liste() {
        synchroniserContrats();
        Map<Integer, Map<StatutTache, Long>> compteurs = new HashMap<>();
        for (Object[] l : tacheRepo.compterParOnboardingEtStatut())
            compteurs.computeIfAbsent((Integer) l[0], k -> new EnumMap<>(StatutTache.class))
                    .put((StatutTache) l[1], (Long) l[2]);

        Map<Integer, Long> retards = new HashMap<>();
        for (Object[] l : tacheRepo.compterEnRetard(LocalDate.now(), List.of(StatutTache.A_FAIRE, StatutTache.EN_COURS)))
            retards.put((Integer) l[0], (Long) l[1]);

        return onboardingRepo.findAllAvecEmploye().stream().map(o -> {
            Map<StatutTache, Long> c = compteurs.getOrDefault(o.getId(), Map.of());
            long total = c.values().stream().mapToLong(Long::longValue).sum() - c.getOrDefault(StatutTache.ANNULE, 0L);
            Employe e = o.getEmploye();
            return new OnboardingResumeDTO(o.getId(), e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(),
                    e.getPoste().getNom(), e.getDepartement().getNom(), o.getDateDebut(), o.getDateFinPrevue(),
                    o.getStatut(), total, c.getOrDefault(StatutTache.FAIT, 0L), retards.getOrDefault(o.getId(), 0L));
        }).toList();
    }

    public OnboardingDetailDTO detail(Integer id) {
        synchroniserContrats();
        Onboarding o = trouver(id);
        Employe e = o.getEmploye();
        List<TacheOnboardingDTO> taches = tacheRepo.findByOnboarding_IdOrderByDateEcheanceAscIdAsc(id).stream()
                .map(this::dto).toList();
        Utilisateur r = o.getResponsable();
        return new OnboardingDetailDTO(o.getId(), e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(),
                e.getEmail(), e.getTelephone(), e.getPoste().getNom(), e.getDepartement().getNom(),
                e.getDateEmbauche(), o.getDateDebut(), o.getDateFinPrevue(), o.getStatut(),
                r == null ? null : r.getPrenom() + " " + r.getNom(), contratRepo.parEmploye(e.getId()), taches,
                documentRepo.findResumesParEmploye(e.getId()));
    }

    // ====================== Tâches ======================

    public TacheOnboardingDTO changerStatutTache(Integer tacheId, TacheStatutRequest req) {
        TacheOnboarding t = tacheRepo.findByIdAvecOnboarding(tacheId)
                .orElseThrow(() -> new RessourceIntrouvableException("Tâche introuvable : " + tacheId));
        if (t.getStatut() == req.statut())
            throw new RegleMetierException(HttpStatus.CONFLICT, "La tâche est déjà dans ce statut.");
        t.setStatut(req.statut());
        // La base impose : date_realisation renseignée si et seulement si statut = FAIT.
        t.setDateRealisation(req.statut() == StatutTache.FAIT ? OffsetDateTime.now() : null);
        if (req.remarque() != null)
            t.setRemarque(req.remarque().isBlank() ? null : req.remarque().trim());
        tacheRepo.save(t);
        recalculerStatut(t.getOnboarding());
        return dto(t);
    }

    public TacheOnboardingDTO ajouterTache(Integer onboardingId, TacheCreationRequest req) {
        Onboarding o = trouver(onboardingId);
        TacheOnboarding t = new TacheOnboarding();
        t.setOnboarding(o);
        t.setResponsable(o.getResponsable());
        t.setLibelle(req.libelle().trim());
        t.setCategorie(req.categorie() == null ? CategorieOnboarding.AUTRE : req.categorie());
        t.setDateEcheance(req.dateEcheance() != null ? req.dateEcheance()
                : o.getDateFinPrevue() != null ? o.getDateFinPrevue() : o.getDateDebut());
        t.setStatut(StatutTache.A_FAIRE);
        tacheRepo.save(t);
        recalculerStatut(o);
        return dto(t);
    }

    /** TERMINE si toutes les tâches non annulées sont faites ; EN_COURS dès qu'une tâche est lancée ou faite. */
    private void recalculerStatut(Onboarding o) {
        List<TacheOnboarding> taches = tacheRepo.findByOnboarding_IdOrderByDateEcheanceAscIdAsc(o.getId());
        if (taches.isEmpty())
            return;
        List<TacheOnboarding> actives = taches.stream().filter(t -> t.getStatut() != StatutTache.ANNULE).toList();
        StatutOnboarding nouveau;
        if (actives.isEmpty() || actives.stream().allMatch(t -> t.getStatut() == StatutTache.FAIT))
            nouveau = StatutOnboarding.TERMINE;
        else if (actives.stream().anyMatch(t -> t.getStatut() == StatutTache.FAIT || t.getStatut() == StatutTache.EN_COURS))
            nouveau = StatutOnboarding.EN_COURS;
        else
            nouveau = StatutOnboarding.A_FAIRE;
        if (o.getStatut() != nouveau) {
            o.setStatut(nouveau);
            onboardingRepo.save(o);
        }
    }

    // ====================== Documents ======================

    public DocumentEmployeDTO ajouterDocument(Integer onboardingId, String type, String nomFichier, byte[] bytes) {
        Onboarding o = trouver(onboardingId);
        TypeDocument typeDocument;
        try {
            typeDocument = TypeDocument.valueOf(type == null ? "" : type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Type de document inconnu : " + type);
        }
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("Le fichier est vide.");
        if (bytes.length > TAILLE_MAX)
            throw new IllegalArgumentException("Le fichier dépasse 5 Mo.");
        String mime = detecterMime(bytes);
        if (mime == null)
            throw new IllegalArgumentException("Format non accepté : joignez un PDF, un JPEG ou un PNG.");

        DocumentEmploye d = new DocumentEmploye();
        d.setEmploye(o.getEmploye());
        d.setTypeDocument(typeDocument);
        d.setNomFichier(nettoyerNom(nomFichier));
        d.setMimeType(mime);
        d.setBytes(bytes);
        return dto(documentRepo.save(d));
    }

    public DocumentEmployeDTO changerStatutDocument(Integer docId, StatutDocument statut) {
        if (statut != StatutDocument.VALIDE && statut != StatutDocument.REFUSE)
            throw new IllegalArgumentException("Statut invalide : seuls VALIDE et REFUSE sont acceptés.");
        DocumentEmploye d = trouverDocument(docId);
        if (d.getStatut() != StatutDocument.FOURNI)
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Ce document a déjà été traité (statut actuel : " + d.getStatut() + ").");
        d.setStatut(statut);
        return dto(documentRepo.save(d));
    }

    public void supprimerDocument(Integer docId) {
        DocumentEmploye d = trouverDocument(docId);
        if (d.getStatut() == StatutDocument.VALIDE)
            throw new RegleMetierException(HttpStatus.CONFLICT, "Un document validé ne peut pas être supprimé.");
        documentRepo.delete(d);
    }

    @Transactional(readOnly = true)
    public DocumentEmploye fichierDocument(Integer docId) {
        return trouverDocument(docId);
    }

    // ====================== Helpers ======================

    private Onboarding trouver(Integer id) {
        return onboardingRepo.findByIdAvecDetails(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Onboarding introuvable : " + id));
    }

    private DocumentEmploye trouverDocument(Integer id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Document introuvable : " + id));
    }

    /** Le type MIME est déduit du contenu (pas du Content-Type envoyé par le client). */
    private String detecterMime(byte[] b) {
        String debut = new String(b, 0, Math.min(b.length, 1024), java.nio.charset.StandardCharsets.ISO_8859_1);
        if (debut.contains("%PDF-"))
            return "application/pdf";
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF)
            return "image/jpeg";
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G')
            return "image/png";
        return null;
    }

    private String nettoyerNom(String nom) {
        String n = nom == null ? "" : nom.substring(Math.max(nom.lastIndexOf('/'), nom.lastIndexOf('\\')) + 1)
                .replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (n.isEmpty())
            n = "document";
        return n.length() > 255 ? n.substring(0, 255) : n;
    }

    private TacheOnboardingDTO dto(TacheOnboarding t) {
        Utilisateur r = t.getResponsable();
        boolean enRetard = t.getDateEcheance() != null && t.getDateEcheance().isBefore(LocalDate.now())
                && (t.getStatut() == StatutTache.A_FAIRE || t.getStatut() == StatutTache.EN_COURS);
        return new TacheOnboardingDTO(t.getId(), t.getOnboarding().getId(), t.getLibelle(), t.getCategorie(),
                t.getDateEcheance(), t.getStatut(), t.getDateRealisation(), t.getRemarque(),
                r == null ? null : r.getPrenom() + " " + r.getNom(), enRetard);
    }

    private DocumentEmployeDTO dto(DocumentEmploye d) {
        return new DocumentEmployeDTO(d.getId(), d.getTypeDocument(), d.getNomFichier(), d.getMimeType(),
                d.getStatut(), d.getDateDepot());
    }
}

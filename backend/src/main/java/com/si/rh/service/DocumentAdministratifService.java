package com.si.rh.service;

import com.si.rh.dto.document.*;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.DocumentEmploye;
import com.si.rh.model.Employe;
import com.si.rh.model.enums.TypeDocument;
import com.si.rh.repository.DocumentEmployeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.utils.Fuseau;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class DocumentAdministratifService {

    public static final String VALIDE = "VALIDE";
    public static final String BIENTOT_EXPIRE = "BIENTOT_EXPIRE";
    public static final String EXPIRE = "EXPIRE";
    public static final String RENOUVELE = "RENOUVELE";
    public static final String SANS_EXPIRATION = "SANS_EXPIRATION";

    private static final int TAILLE_MAX = 5 * 1024 * 1024;
    private static final int DELAI_DEFAUT = 30;
    private static final int DELAI_MAX = 365;

    private final DocumentEmployeRepository documentRepository;
    private final EmployeRepository employeRepository;
    private final AuditContexte audit;

    public DocumentAdministratifService(DocumentEmployeRepository documentRepository,
            EmployeRepository employeRepository, AuditContexte audit) {
        this.documentRepository = documentRepository;
        this.employeRepository = employeRepository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DocumentAdminDTO> liste(Integer employeId) {
        if (!employeRepository.existsById(employeId)) {
            throw new RessourceIntrouvableException("Employé introuvable : " + employeId);
        }
        LocalDate aujourdhui = Fuseau.aujourdhui();
        return documentRepository.findLignesAdminParEmploye(employeId).stream()
                .map(l -> versDto(l, aujourdhui)).toList();
    }

    @Transactional(readOnly = true)
    public DocumentEmploye fichier(Integer docId) {
        return trouver(docId);
    }

    public DocumentAdminDTO deposer(Integer employeId, String type, String nomFichier, byte[] bytes,
            LocalDate dateExpiration, Integer delaiAlerteJours) {
        audit.appliquer();
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable : " + employeId));

        TypeDocument typeDocument;
        try {
            typeDocument = TypeDocument.valueOf(type == null ? "" : type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Type de document inconnu : " + type);
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        if (bytes.length > TAILLE_MAX) {
            throw new IllegalArgumentException("Le fichier dépasse 5 Mo.");
        }
        String mime = detecterMime(bytes);
        if (mime == null) {
            throw new IllegalArgumentException("Format non accepté : joignez un PDF, un JPEG ou un PNG.");
        }
        if (dateExpiration != null && dateExpiration.isBefore(Fuseau.aujourdhui())) {
            throw new IllegalArgumentException("La date d'expiration ne peut pas être dans le passé.");
        }

        DocumentEmploye d = new DocumentEmploye();
        d.setEmploye(employe);
        d.setTypeDocument(typeDocument);
        d.setNomFichier(nettoyerNom(nomFichier));
        d.setMimeType(mime);
        d.setBytes(bytes);
        d.setDateExpiration(dateExpiration);
        d.setDelaiAlerteJours(delaiValide(delaiAlerteJours, DELAI_DEFAUT));
        return versDto(documentRepository.save(d));
    }

    public DocumentAdminDTO modifierExpiration(Integer docId, DocumentExpirationRequest req) {
        audit.appliquer();
        DocumentEmploye d = trouver(docId);
        if (req.dateExpiration() != null && d.getDateDepot() != null
                && req.dateExpiration().isBefore(d.getDateDepot().atZoneSameInstant(Fuseau.MADAGASCAR).toLocalDate())) {
            throw new IllegalArgumentException("La date d'expiration ne peut pas précéder la date de dépôt du document.");
        }
        d.setDateExpiration(req.dateExpiration());
        d.setDelaiAlerteJours(delaiValide(req.delaiAlerteJours(), d.getDelaiAlerteJours()));
        return versDto(documentRepository.save(d));
    }

    public DocumentAdminDTO marquerRenouvele(Integer docId, LocalDate dateRenouvellement) {
        audit.appliquer();
        DocumentEmploye d = trouver(docId);
        if (d.getDateExpiration() == null) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce document n'a pas de date d'expiration.");
        }
        if (d.getDateRenouvellement() != null) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce document est déjà marqué comme renouvelé.");
        }
        LocalDate date = dateRenouvellement == null ? Fuseau.aujourdhui() : dateRenouvellement;
        if (date.isAfter(Fuseau.aujourdhui())) {
            throw new IllegalArgumentException("La date de renouvellement ne peut pas être dans le futur.");
        }
        d.setDateRenouvellement(date);
        return versDto(documentRepository.save(d));
    }

    public static String etatValidite(LocalDate expiration, LocalDate renouvellement, Integer delai,
            LocalDate aujourdhui) {
        if (expiration == null) {
            return SANS_EXPIRATION;
        }
        if (renouvellement != null) {
            return RENOUVELE;
        }
        if (expiration.isBefore(aujourdhui)) {
            return EXPIRE;
        }
        int d = delai == null ? DELAI_DEFAUT : delai;
        return aujourdhui.isBefore(expiration.minusDays(d)) ? VALIDE : BIENTOT_EXPIRE;
    }

    public static Long joursRestants(LocalDate expiration, LocalDate aujourdhui) {
        return expiration == null ? null : ChronoUnit.DAYS.between(aujourdhui, expiration);
    }

    static DocumentAdminDTO versDto(DocumentAdminLigne l, LocalDate aujourdhui) {
        return new DocumentAdminDTO(l.id(), l.typeDocument(), l.nomFichier(), l.mimeType(), l.statut(),
                l.dateDepot(), l.dateExpiration(), l.dateRenouvellement(), l.delaiAlerteJours(),
                etatValidite(l.dateExpiration(), l.dateRenouvellement(), l.delaiAlerteJours(), aujourdhui),
                joursRestants(l.dateExpiration(), aujourdhui));
    }

    private DocumentAdminDTO versDto(DocumentEmploye d) {
        return versDto(new DocumentAdminLigne(d.getId(), d.getTypeDocument(), d.getNomFichier(), d.getMimeType(),
                d.getStatut(), d.getDateDepot(), d.getDateExpiration(), d.getDateRenouvellement(),
                d.getDelaiAlerteJours()), Fuseau.aujourdhui());
    }

    private DocumentEmploye trouver(Integer id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Document introuvable : " + id));
    }

    private int delaiValide(Integer demande, Integer parDefaut) {
        int valeur = demande != null ? demande : (parDefaut != null ? parDefaut : DELAI_DEFAUT);
        if (valeur < 0 || valeur > DELAI_MAX) {
            throw new IllegalArgumentException("Le délai d'alerte doit être compris entre 0 et " + DELAI_MAX + " jours.");
        }
        return valeur;
    }

    private String detecterMime(byte[] b) {
        String debut = new String(b, 0, Math.min(b.length, 1024), StandardCharsets.ISO_8859_1);
        if (debut.contains("%PDF-")) {
            return "application/pdf";
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        return null;
    }

    private String nettoyerNom(String nom) {
        String n = nom == null ? "" : nom.substring(Math.max(nom.lastIndexOf('/'), nom.lastIndexOf('\\')) + 1)
                .replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (n.isEmpty()) {
            n = "document";
        }
        return n.length() > 255 ? n.substring(0, 255) : n;
    }
}

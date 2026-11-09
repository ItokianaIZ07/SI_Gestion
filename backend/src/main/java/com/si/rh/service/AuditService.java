package com.si.rh.service;

import com.si.rh.dto.PageResultat;
import com.si.rh.dto.audit.AuditDTO;
import com.si.rh.dto.audit.UtilisateurAuditDTO;
import com.si.rh.model.JournalAudit;
import com.si.rh.model.Utilisateur;
import com.si.rh.repository.JournalAuditRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.utils.Fuseau;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuditService {

    private static final int TAILLE_MAX = 200;

    private final JournalAuditRepository journalRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AuditService(JournalAuditRepository journalRepository, UtilisateurRepository utilisateurRepository) {
        this.journalRepository = journalRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public PageResultat<AuditDTO> rechercher(Integer idUtilisateur, String table, String enregistrement,
            String action, LocalDate du, LocalDate au, int page, int taille) {
        int p = Math.max(page, 1);
        int t = Math.min(Math.max(taille, 1), TAILLE_MAX);

        Specification<JournalAudit> spec = (root, query, cb) -> {
            List<Predicate> filtres = new ArrayList<>();
            if (idUtilisateur != null) {
                filtres.add(cb.equal(root.get("idUtilisateur"), idUtilisateur));
            }
            if (renseigne(table)) {
                filtres.add(cb.equal(root.get("tableConcernee"), table.trim()));
            }
            if (renseigne(enregistrement)) {
                filtres.add(cb.equal(root.get("identifiantEnregistrement"), enregistrement.trim()));
            }
            if (renseigne(action)) {
                filtres.add(cb.equal(root.get("action"), action.trim().toUpperCase()));
            }
            if (du != null) {
                OffsetDateTime debut = du.atStartOfDay(Fuseau.MADAGASCAR).toOffsetDateTime();
                filtres.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("dateAction"), debut));
            }
            if (au != null) {
                OffsetDateTime fin = au.plusDays(1).atStartOfDay(Fuseau.MADAGASCAR).toOffsetDateTime();
                filtres.add(cb.lessThan(root.<OffsetDateTime>get("dateAction"), fin));
            }
            return cb.and(filtres.toArray(new Predicate[0]));
        };

        Page<JournalAudit> resultat = journalRepository.findAll(spec,
                PageRequest.of(p - 1, t, Sort.by(Sort.Direction.DESC, "dateAction").and(Sort.by(Sort.Direction.DESC, "id"))));

        Set<Integer> ids = resultat.getContent().stream().map(JournalAudit::getIdUtilisateur)
                .filter(i -> i != null).collect(Collectors.toSet());
        Map<Integer, String> noms = new HashMap<>();
        for (Utilisateur u : utilisateurRepository.findAllById(ids)) {
            noms.put(u.getId(), u.getPrenom() + " " + u.getNom());
        }

        List<AuditDTO> contenu = resultat.getContent().stream()
                .map(a -> new AuditDTO(a.getId(), a.getIdUtilisateur(),
                        a.getIdUtilisateur() == null ? null : noms.get(a.getIdUtilisateur()), a.getAction(),
                        a.getTableConcernee(), a.getIdentifiantEnregistrement(), a.getAnciennesValeurs(),
                        a.getNouvellesValeurs(), a.getAdresseIp(), a.getDateAction()))
                .toList();
        return new PageResultat<>(contenu, p, t, resultat.getTotalElements(), resultat.getTotalPages());
    }

    public List<String> tables() {
        return journalRepository.tablesAudit();
    }

    private static boolean renseigne(String s) {
        return s != null && !s.isBlank();
    }

    public List<UtilisateurAuditDTO> utilisateurs() {
        return utilisateurRepository.findAll().stream()
                .map(u -> new UtilisateurAuditDTO(u.getId(), u.getPrenom() + " " + u.getNom(), u.getEmail()))
                .sorted((a, b) -> a.nomComplet().compareToIgnoreCase(b.nomComplet()))
                .toList();
    }
}

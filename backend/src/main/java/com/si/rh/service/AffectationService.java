package com.si.rh.service;

import com.si.rh.dto.fiche.AffectationDTO;
import com.si.rh.dto.fiche.ChangementAffectationRequest;
import com.si.rh.dto.fiche.ReferentielAffectationDTO;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Employe;
import com.si.rh.model.HistoriqueAffectation;
import com.si.rh.model.Poste;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.HistoriqueAffectationRepository;
import com.si.rh.repository.PosteRepository;
import com.si.rh.utils.Fuseau;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class AffectationService {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HistoriqueAffectationRepository historiqueRepository;
    private final EmployeRepository employeRepository;
    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final JdbcTemplate jdbc;
    private final AuditContexte audit;

    @PersistenceContext
    private EntityManager entityManager;

    public AffectationService(HistoriqueAffectationRepository historiqueRepository,
            EmployeRepository employeRepository, PosteRepository posteRepository,
            DepartementRepository departementRepository, JdbcTemplate jdbc, AuditContexte audit) {
        this.historiqueRepository = historiqueRepository;
        this.employeRepository = employeRepository;
        this.posteRepository = posteRepository;
        this.departementRepository = departementRepository;
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AffectationDTO> carriere(Integer employeId) {
        if (!employeRepository.existsById(employeId)) {
            throw new RessourceIntrouvableException("Employé introuvable : " + employeId);
        }
        return historiqueRepository.carriere(employeId).stream().map(AffectationService::versDto).toList();
    }

    @Transactional(readOnly = true)
    public ReferentielAffectationDTO referentiels() {
        var departements = departementRepository.findAll().stream()
                .map(d -> new ReferentielAffectationDTO.DepartementRef(d.getId(), d.getNom()))
                .sorted((a, b) -> a.nom().compareToIgnoreCase(b.nom())).toList();
        var postes = posteRepository.findActifsAvecDepartement().stream()
                .map(p -> new ReferentielAffectationDTO.PosteRef(p.getId(), p.getNom(), p.getDepartement().getId()))
                .toList();
        var employes = employeRepository.findByStatutOrderByNomAscPrenomAsc(StatutEmploye.ACTIF).stream()
                .map(e -> new ReferentielAffectationDTO.EmployeRef(e.getId(), e.getPrenom() + " " + e.getNom(),
                        e.getMatricule()))
                .toList();
        return new ReferentielAffectationDTO(departements, postes, employes);
    }

    public List<AffectationDTO> changer(Integer employeId, ChangementAffectationRequest req) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable : " + employeId));
        if (employe.getStatut() == StatutEmploye.PARTI) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Un employé parti ne peut plus être réaffecté.");
        }

        Poste poste = posteRepository.findById(req.idPoste())
                .orElseThrow(() -> new RessourceIntrouvableException("Poste introuvable : " + req.idPoste()));
        if (!poste.getDepartement().getId().equals(req.idDepartement())) {
            throw new RegleMetierException(HttpStatus.CONFLICT,
                    "Le poste choisi n'appartient pas au département sélectionné.");
        }
        if (!Boolean.TRUE.equals(poste.getActif())) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce poste n'est plus actif.");
        }

        if (req.idSuperieur() != null) {
            if (req.idSuperieur().equals(employeId)) {
                throw new RegleMetierException(HttpStatus.CONFLICT, "Un employé ne peut pas être son propre supérieur.");
            }
            Employe superieur = employeRepository.findById(req.idSuperieur())
                    .orElseThrow(() -> new RessourceIntrouvableException("Supérieur introuvable : " + req.idSuperieur()));
            if (superieur.getStatut() == StatutEmploye.PARTI) {
                throw new RegleMetierException(HttpStatus.CONFLICT, "Le supérieur choisi a quitté l'entreprise.");
            }
        }

        LocalDate dateEffet = req.dateEffet() == null ? Fuseau.aujourdhui() : req.dateEffet();
        HistoriqueAffectation courante = historiqueRepository.courante(employeId).orElse(null);
        if (courante != null) {
            if (dateEffet.isBefore(courante.getDateDebut())) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                        "La date d'effet ne peut pas précéder le début de l'affectation actuelle ("
                                + courante.getDateDebut().format(FORMAT) + ").");
            }
            Integer superieurActuel = courante.getSuperieur() == null ? null : courante.getSuperieur().getId();
            boolean identique = employe.getDepartement().getId().equals(req.idDepartement())
                    && employe.getPoste().getId().equals(req.idPoste())
                    && (req.idSuperieur() == null || req.idSuperieur().equals(superieurActuel));
            if (identique) {
                throw new RegleMetierException(HttpStatus.CONFLICT,
                        "Aucun changement : l'affectation demandée est identique à l'affectation actuelle.");
            }
        }

        String motif = req.motif() == null || req.motif().isBlank() ? null : req.motif().trim();
        audit.appliquer();
        jdbc.queryForObject(
                "SELECT changer_affectation(?, ?, ?, CAST(? AS integer), CAST(? AS date), CAST(? AS text))::text",
                String.class, employeId, req.idDepartement(), req.idPoste(), req.idSuperieur(),
                java.sql.Date.valueOf(dateEffet), motif);

        entityManager.clear();
        return carriere(employeId);
    }

    static AffectationDTO versDto(HistoriqueAffectation h) {
        var sup = h.getSuperieur();
        return new AffectationDTO(h.getId(), h.getDepartement().getId(), h.getDepartement().getNom(),
                h.getPoste().getId(), h.getPoste().getNom(), sup == null ? null : sup.getId(),
                sup == null ? null : sup.getPrenom() + " " + sup.getNom(), h.getDateDebut(), h.getDateFin(),
                h.getMotif(), h.getDateFin() == null);
    }
}

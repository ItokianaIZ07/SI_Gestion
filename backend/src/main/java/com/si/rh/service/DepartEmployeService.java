package com.si.rh.service;

import com.si.rh.dto.fiche.DepartEmployeControleDTO;
import com.si.rh.dto.fiche.DepartEmployeRequest;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.DepartEmployeRepository;
import com.si.rh.utils.Fuseau;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class DepartEmployeService {

    private final JdbcTemplate jdbcTemplate;
    private final DepartEmployeRepository departEmployeRepository;
    private final AuditContexte auditContexte;

    public DepartEmployeService(
            JdbcTemplate jdbcTemplate,
            DepartEmployeRepository departEmployeRepository,
            AuditContexte auditContexte) {
        this.jdbcTemplate = jdbcTemplate;
        this.departEmployeRepository = departEmployeRepository;
        this.auditContexte = auditContexte;
    }

    public DepartEmployeControleDTO getControle(Integer idEmploye) {
        verifierEmploye(idEmploye);
        return departEmployeRepository.getControle(idEmploye);
    }

    @Transactional
    public void marquerParti(Integer idEmploye, DepartEmployeRequest demande) {
        EmployeDepart employe = jdbcTemplate.query("""
                SELECT date_embauche, statut
                FROM employe
                WHERE id = ?
                FOR UPDATE
                """, rs -> rs.next()
                        ? new EmployeDepart(rs.getObject("date_embauche", LocalDate.class),
                                StatutEmploye.valueOf(rs.getString("statut")))
                        : null,
                idEmploye);

        if (employe == null) {
            throw new RessourceIntrouvableException("Employé introuvable : " + idEmploye);
        }
        if (employe.statut() == StatutEmploye.PARTI) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce salarié est déjà marqué comme parti.");
        }
        if (demande.dateDepart().isBefore(employe.dateEmbauche())) {
            throw new RegleMetierException(
                    HttpStatus.BAD_REQUEST, "La date de départ ne peut pas précéder la date d'embauche.");
        }
        if (demande.dateDepart().isAfter(Fuseau.aujourdhui())) {
            throw new RegleMetierException(
                    HttpStatus.BAD_REQUEST, "La date de départ ne peut pas être dans le futur.");
        }

        auditContexte.appliquer();
        int lignesModifiees = jdbcTemplate.update("""
                UPDATE employe
                SET statut = 'PARTI', date_depart = ?
                WHERE id = ? AND statut <> 'PARTI'
                """, demande.dateDepart(), idEmploye);
        if (lignesModifiees != 1) {
            throw new RegleMetierException(HttpStatus.CONFLICT, "Ce salarié est déjà marqué comme parti.");
        }
    }

    private void verifierEmploye(Integer idEmploye) {
        Boolean existe = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM employe WHERE id = ?)",
                Boolean.class,
                idEmploye);
        if (!Boolean.TRUE.equals(existe)) {
            throw new RessourceIntrouvableException("Employé introuvable : " + idEmploye);
        }
    }

    private record EmployeDepart(LocalDate dateEmbauche, StatutEmploye statut) {
    }
}

package com.si.rh.repository;

import com.si.rh.dto.fiche.DepartEmployeControleDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DepartEmployeRepository {

    private final JdbcTemplate jdbcTemplate;

    public DepartEmployeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DepartEmployeControleDTO getControle(Integer idEmploye) {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM lister_subordonnes(?, TRUE)) AS nb_subordonnes_directs,
                    EXISTS (
                        SELECT 1
                        FROM contrat c
                        JOIN statut_contract s ON s.id = c.id_statut_contrat
                        WHERE c.id_employe = ? AND s.code = 'ACTIF'
                    ) AS contrat_actif,
                    (SELECT COUNT(*)
                     FROM demande_conge
                     WHERE id_employe = ?
                       AND statut IN ('EN_ATTENTE_MANAGER', 'EN_ATTENTE_RH')) AS demandes_conge_en_attente,
                    (SELECT COUNT(*)
                     FROM mission
                     WHERE id_employe = ?
                       AND statut = 'EN_COURS'
                       AND date_debut <= CURRENT_DATE
                       AND date_fin >= CURRENT_DATE) AS missions_en_cours
                """, (rs, rowNum) -> new DepartEmployeControleDTO(
                rs.getLong("nb_subordonnes_directs"),
                rs.getBoolean("contrat_actif"),
                rs.getLong("demandes_conge_en_attente"),
                rs.getLong("missions_en_cours")),
                idEmploye, idEmploye, idEmploye, idEmploye);
    }
}

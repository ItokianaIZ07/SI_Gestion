package com.si.rh.repository;

import com.si.rh.dto.onboarding.HistoriqueTacheOnboardingDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HistoriqueTacheOnboardingRepository {

    private final JdbcTemplate jdbcTemplate;

    public HistoriqueTacheOnboardingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HistoriqueTacheOnboardingDTO> trouverParTache(Integer tacheId) {
        return jdbcTemplate.query("""
                SELECT h.id, h.ancien_statut, h.nouveau_statut,
                       h.ancienne_echeance, h.nouvelle_echeance,
                       h.ancienne_remarque, h.nouvelle_remarque,
                       CASE
                           WHEN h.id_utilisateur_auteur IS NULL THEN 'Système'
                           WHEN u.id IS NULL THEN 'Inconnu'
                           ELSE concat_ws(' ', u.prenom, u.nom)
                       END AS auteur,
                       h.date_action
                FROM historique_tache_onboarding h
                LEFT JOIN utilisateur u ON u.id = h.id_utilisateur_auteur
                WHERE h.id_tache = ?
                ORDER BY h.date_action DESC, h.id DESC
                """, (rs, rowNum) -> new HistoriqueTacheOnboardingDTO(
                rs.getLong("id"),
                rs.getString("ancien_statut"),
                rs.getString("nouveau_statut"),
                rs.getObject("ancienne_echeance", java.time.LocalDate.class),
                rs.getObject("nouvelle_echeance", java.time.LocalDate.class),
                rs.getString("ancienne_remarque"),
                rs.getString("nouvelle_remarque"),
                rs.getString("auteur"),
                rs.getObject("date_action", java.time.OffsetDateTime.class)),
                tacheId);
    }
}

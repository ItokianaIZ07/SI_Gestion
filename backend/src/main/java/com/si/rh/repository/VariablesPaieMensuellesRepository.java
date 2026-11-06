package com.si.rh.repository;

import com.si.rh.dto.rh.variablesPaie.VariablesPaieBrutes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class VariablesPaieMensuellesRepository {

    private static final String REQUETE_BASE = """
            SELECT v.id_employe,
                   v.matricule,
                   v.nom,
                   v.prenom,
                   d.nom AS departement,
                   v.annee,
                   v.mois,
                   COALESCE(v.minutes_prevues, 0) AS minutes_prevues,
                   COALESCE(v.minutes_travaillees, 0) AS minutes_travaillees,
                   COALESCE(v.minutes_retard, 0) AS minutes_retard,
                   COALESCE(v.nb_retards, 0) AS nb_retards,
                   COALESCE(v.minutes_sup_validees, 0) AS minutes_sup_validees,
                   COALESCE(v.minutes_sup_a_valider, 0) AS minutes_sup_a_valider
            FROM v_variables_paie_mensuelles v
            JOIN employe e ON e.id = v.id_employe
            JOIN departement d ON d.id = e.id_departement
            WHERE v.annee = ?
              AND v.mois = ?
              AND (CAST(? AS INTEGER) IS NULL OR e.id_departement = ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public VariablesPaieMensuellesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VariablesPaieBrutes> findRecapitulatif(
            Integer annee,
            Integer mois,
            Integer idDepartement) {
        return jdbcTemplate.query(
                REQUETE_BASE + " ORDER BY v.nom, v.prenom",
                this::mapper,
                annee, mois, idDepartement, idDepartement);
    }

    public List<VariablesPaieBrutes> findRecapitulatifManager(
            Integer annee,
            Integer mois,
            Integer idDepartement,
            Integer idUtilisateur) {
        return jdbcTemplate.query(
                REQUETE_BASE
                        + " AND utilisateur_est_manager_de(?, v.id_employe)"
                        + " ORDER BY v.nom, v.prenom",
                this::mapper,
                annee, mois, idDepartement, idDepartement, idUtilisateur);
    }

    private VariablesPaieBrutes mapper(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VariablesPaieBrutes(
                resultSet.getInt("id_employe"),
                resultSet.getString("matricule"),
                resultSet.getString("nom"),
                resultSet.getString("prenom"),
                resultSet.getString("departement"),
                resultSet.getInt("annee"),
                resultSet.getInt("mois"),
                resultSet.getLong("minutes_prevues"),
                resultSet.getLong("minutes_travaillees"),
                resultSet.getLong("minutes_retard"),
                resultSet.getLong("nb_retards"),
                resultSet.getLong("minutes_sup_validees"),
                resultSet.getLong("minutes_sup_a_valider"));
    }
}

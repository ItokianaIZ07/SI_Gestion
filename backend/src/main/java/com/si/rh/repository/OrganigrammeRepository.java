package com.si.rh.repository;

import com.si.rh.dto.rh.organigramme.DepartementOrganigramme;
import com.si.rh.dto.rh.organigramme.OrganigrammeLigne;
import com.si.rh.dto.rh.organigramme.OrganigrammePersonne;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class OrganigrammeRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrganigrammeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OrganigrammeLigne> findOrganigramme() {
        return jdbcTemplate.query("""
                SELECT id_employe, matricule, nom, prenom, departement, poste,
                       id_superieur, nom_superieur, prenom_superieur, nb_subordonnes_directs
                FROM v_organigramme
                ORDER BY departement, nom, prenom
                """, this::mapLigne);
    }

    public List<Integer> findIdsOrganigramme() {
        return jdbcTemplate.queryForList(
                "SELECT id_employe FROM v_organigramme", Integer.class);
    }

    public List<OrganigrammeLigne> findOrganigrammeManager(Integer idUtilisateur, Integer idEmployeManager) {
        return jdbcTemplate.query("""
                SELECT o.id_employe, o.matricule, o.nom, o.prenom, o.departement, o.poste,
                       o.id_superieur, o.nom_superieur, o.prenom_superieur, o.nb_subordonnes_directs
                FROM v_organigramme o
                WHERE o.id_employe = ?
                   OR utilisateur_est_manager_de(?, o.id_employe)
                ORDER BY o.departement, o.nom, o.prenom
                """, this::mapLigne, idEmployeManager, idUtilisateur);
    }

    public List<DepartementOrganigramme> findDepartements() {
        return jdbcTemplate.query("""
                SELECT id, nom
                FROM departement
                ORDER BY nom
                """, (rs, row) -> new DepartementOrganigramme(
                rs.getInt("id"), rs.getString("nom")));
    }

    public List<DepartementOrganigramme> findDepartementsManager(Integer idUtilisateur, Integer idEmployeManager) {
        return jdbcTemplate.query("""
                SELECT DISTINCT d.id, d.nom
                FROM v_organigramme o
                JOIN historique_affectation h
                  ON h.id_employe = o.id_employe AND h.date_fin IS NULL
                JOIN departement d ON d.id = h.id_departement
                WHERE o.id_employe = ?
                   OR utilisateur_est_manager_de(?, o.id_employe)
                ORDER BY d.nom
                """, (rs, row) -> new DepartementOrganigramme(
                rs.getInt("id"), rs.getString("nom")), idEmployeManager, idUtilisateur);
    }

    public Optional<Integer> findIdEmployeByIdUtilisateur(Integer idUtilisateur) {
        return jdbcTemplate.query("""
                SELECT id FROM employe WHERE id_utilisateur = ?
                """, rs -> rs.next() ? Optional.of(rs.getInt("id")) : Optional.empty(), idUtilisateur);
    }

    public Optional<OrganigrammeLigne> findEmploye(Integer idEmploye) {
        List<OrganigrammeLigne> lignes = jdbcTemplate.query("""
                SELECT id_employe, matricule, nom, prenom, departement, poste,
                       id_superieur, nom_superieur, prenom_superieur, nb_subordonnes_directs
                FROM v_organigramme
                WHERE id_employe = ?
                """, this::mapLigne, idEmploye);
        return lignes.stream().findFirst();
    }

    public boolean estDansEquipeManager(Integer idUtilisateur, Integer idEmployeManager, Integer idEmploye) {
        Boolean estDansEquipe = jdbcTemplate.queryForObject("""
                SELECT (CAST(? AS INTEGER) = CAST(? AS INTEGER)
                        OR utilisateur_est_manager_de(CAST(? AS INTEGER), CAST(? AS INTEGER)))
                """, Boolean.class, idEmployeManager, idEmploye, idUtilisateur, idEmploye);
        return Boolean.TRUE.equals(estDansEquipe);
    }

    public List<OrganigrammePersonne> findChaineSuperieurs(Integer idEmploye) {
        return jdbcTemplate.query("""
                SELECT o.id_employe, o.matricule, o.nom, o.prenom, o.departement, o.poste,
                       s.sup_niveau AS niveau
                FROM lister_superieurs(?) s
                JOIN v_organigramme o ON o.id_employe = s.sup_id
                ORDER BY s.sup_niveau
                """, this::mapPersonne, idEmploye);
    }

    public List<OrganigrammePersonne> findSubordonnes(Integer idEmploye, boolean directsSeulement) {
        return jdbcTemplate.query("""
                SELECT o.id_employe, o.matricule, o.nom, o.prenom, o.departement, o.poste,
                       s.sub_niveau AS niveau
                FROM lister_subordonnes(?, ?) s
                JOIN v_organigramme o ON o.id_employe = s.sub_id
                ORDER BY s.sub_niveau, o.nom, o.prenom
                """, this::mapPersonne, idEmploye, directsSeulement);
    }

    private OrganigrammeLigne mapLigne(ResultSet rs, int rowNum) throws SQLException {
        return new OrganigrammeLigne(
                rs.getInt("id_employe"),
                rs.getString("matricule"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("departement"),
                rs.getString("poste"),
                nullableInt(rs, "id_superieur"),
                rs.getString("nom_superieur"),
                rs.getString("prenom_superieur"),
                rs.getLong("nb_subordonnes_directs"));
    }

    private OrganigrammePersonne mapPersonne(ResultSet rs, int rowNum) throws SQLException {
        return new OrganigrammePersonne(
                rs.getInt("id_employe"),
                rs.getString("matricule"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("departement"),
                rs.getString("poste"),
                rs.getInt("niveau"));
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int valeur = rs.getInt(column);
        return rs.wasNull() ? null : valeur;
    }
}

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

    private static final String EQUIPE_MANAGER_CTE = """
            WITH RECURSIVE racines(id_employe, chemin) AS (
                SELECT e.id, ARRAY[e.id]
                FROM employe e
                WHERE e.id_utilisateur = ? AND e.statut <> 'PARTI'
                UNION
                SELECT h.id_employe, ARRAY[h.id_employe]
                FROM historique_affectation h
                JOIN departement d ON d.id = h.id_departement
                WHERE h.date_fin IS NULL AND d.id_utilisateur_manager = ?
                UNION
                SELECT h.id_employe, ARRAY[h.id_employe]
                FROM historique_affectation h
                JOIN employe s ON s.id = h.id_superieur
                WHERE h.date_fin IS NULL AND s.id_utilisateur = ?
            ),
            equipe(id_employe, chemin) AS (
                SELECT id_employe, chemin FROM racines
                UNION ALL
                SELECT h.id_employe, equipe.chemin || h.id_employe
                FROM equipe
                JOIN historique_affectation h
                  ON h.id_superieur = equipe.id_employe
                 AND h.date_fin IS NULL
                WHERE NOT h.id_employe = ANY(equipe.chemin)
            )
            """;

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

    public List<OrganigrammeLigne> findOrganigrammeManager(Integer idUtilisateur) {
        return jdbcTemplate.query(EQUIPE_MANAGER_CTE + """
                SELECT o.id_employe, o.matricule, o.nom, o.prenom, o.departement, o.poste,
                       o.id_superieur, o.nom_superieur, o.prenom_superieur, o.nb_subordonnes_directs
                FROM v_organigramme o
                JOIN equipe e ON e.id_employe = o.id_employe
                ORDER BY o.departement, o.nom, o.prenom
                """, this::mapLigne, idUtilisateur, idUtilisateur, idUtilisateur);
    }

    public List<DepartementOrganigramme> findDepartements() {
        return jdbcTemplate.query("""
                SELECT id, nom
                FROM departement
                ORDER BY nom
                """, (rs, row) -> new DepartementOrganigramme(
                rs.getInt("id"), rs.getString("nom")));
    }

    public List<DepartementOrganigramme> findDepartementsManager(Integer idUtilisateur) {
        return jdbcTemplate.query(EQUIPE_MANAGER_CTE + """
                SELECT DISTINCT d.id, d.nom
                FROM v_organigramme o
                JOIN equipe e ON e.id_employe = o.id_employe
                JOIN historique_affectation h
                  ON h.id_employe = o.id_employe AND h.date_fin IS NULL
                JOIN departement d ON d.id = h.id_departement
                ORDER BY d.nom
                """, (rs, row) -> new DepartementOrganigramme(
                rs.getInt("id"), rs.getString("nom")), idUtilisateur, idUtilisateur, idUtilisateur);
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

    public boolean estDansEquipeManager(Integer idUtilisateur, Integer idEmploye) {
        Integer membre = jdbcTemplate.query(EQUIPE_MANAGER_CTE + """
                SELECT id_employe
                FROM equipe
                WHERE id_employe = ?
                LIMIT 1
                """, rs -> rs.next() ? rs.getInt("id_employe") : null,
                idUtilisateur, idUtilisateur, idUtilisateur, idEmploye);
        return membre != null;
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

# VUE : vue_solde_conge
#
# Rôle :
# Affiche le solde de chaque employé par type de congé.
#
# Les colonnes :
# - acquis : acquisitions
# - pris : consommations
# - ajustement : corrections
# - restitue : jours restitués
# - expire : jours expirés
# - disponible : solde réel
#
# Aucun solde n'est stocké directement dans cette vue.
# ------------------------------------------------------------

CREATE OR REPLACE VIEW vue_solde_conge AS
SELECT
    e.id AS id_employe,

    tc.id AS id_type_conge,

    tc.code AS type_conge,

    tc.libelle AS libelle_conge,

    COALESCE(
        SUM(m.nombre_jours)
            FILTER (WHERE m.type_mouvement = 'ACQUISITION'),
        0
    ) AS acquis,

    COALESCE(
        -SUM(m.nombre_jours)
            FILTER (WHERE m.type_mouvement = 'CONSOMMATION'),
        0
    ) AS pris,

    COALESCE(
        SUM(m.nombre_jours)
            FILTER (WHERE m.type_mouvement = 'AJUSTEMENT'),
        0
    ) AS ajustement,

    COALESCE(
        SUM(m.nombre_jours)
            FILTER (WHERE m.type_mouvement = 'RESTITUTION'),
        0
    ) AS restitue,

    COALESCE(
        -SUM(m.nombre_jours)
            FILTER (WHERE m.type_mouvement = 'EXPIRATION'),
        0
    ) AS expire,

    COALESCE(SUM(m.nombre_jours), 0) AS disponible

FROM employe e

CROSS JOIN type_conge tc

LEFT JOIN mouvement_conge m
    ON m.id_employe = e.id
   AND m.id_type_conge = tc.id

GROUP BY
    e.id,
    tc.id,
    tc.code,
    tc.libelle;


# ------------------------------------------------------------
# VUE : vue_historique_conge
#
# Rôle :
# Affiche les mouvements avec les informations du salarié
# et du type de congé.
#
# Utile pour la page historique du solde.
# ------------------------------------------------------------

CREATE OR REPLACE VIEW vue_historique_conge AS
SELECT
    m.id,

    m.id_employe,

    m.id_type_conge,

    m.id_demande_conge,

    m.date_mouvement,

    m.type_mouvement,

    m.nombre_jours,

    m.periode_debut,

    m.motif,

    tc.libelle AS type_conge

FROM mouvement_conge m

JOIN type_conge tc
    ON tc.id = m.id_type_conge;


# ------------------------------------------------------------
# VUE : vue_demande_conge
#
# Rôle :
# Affiche les demandes avec les informations de l'employé,
# le type de congé et les éventuels remplacements.
# ------------------------------------------------------------

CREATE OR REPLACE VIEW vue_demande_conge AS
SELECT
    dc.id,

    dc.id_employe,

    dc.id_type_conge,

    dc.date_debut,

    dc.date_fin,

    dc.nombre_jours,

    dc.motif,

    dc.date_demande,

    dc.statut_global,

    tc.code AS code_type_conge,

    tc.libelle AS type_conge,

    rc.id AS id_remplacement,

    rc.id_employe_remplacant,

    rc.decision AS decision_remplacement,

    rc.commentaire AS commentaire_remplacement

FROM demande_conge dc

JOIN type_conge tc
    ON tc.id = dc.id_type_conge

LEFT JOIN remplacement_conge rc
    ON rc.id_demande_conge = dc.id;


# ------------------------------------------------------------
# VUE : vue_validation_conge
#
# Rôle :
# Affiche le circuit de validation de chaque demande.
#
# La décision est récupérée dans la table existante decision.
# ------------------------------------------------------------

CREATE OR REPLACE VIEW vue_validation_conge AS
SELECT
    vc.id,

    vc.id_demande_conge,

    vc.id_utilisateur,

    vc.etape,

    vc.ordre,

    vc.id_decision,

    d.code AS decision,

    vc.commentaire,

    vc.date_validation

FROM validation_conge vc

JOIN decision d
    ON d.id = vc.id_decision;


# ------------------------------------------------------------
# VUE : vue_absence
#
# Rôle :
# Affiche les absences avec leur type et leur statut.
# ------------------------------------------------------------

CREATE OR REPLACE VIEW vue_absence AS
SELECT
    a.id,

    a.id_employe,

    a.id_type_absence,

    ta.code AS code_type_absence,

    ta.libelle AS type_absence,

    a.date_debut,

    a.date_fin,

    a.motif,

    a.statut,

    a.date_declaration,

    a.id_utilisateur_verificateur,

    a.commentaire_rh

FROM absence a

JOIN type_absence ta
    ON ta.id = a.id_type_absence;


# ------------------------------------------------------------
# VUE : vue_planning_rh
#
# Rôle :
# Centralise les congés validés, les absences et les missions.
#
# Cette vue peut alimenter un calendrier RH.
#
# Le type_evenement permet de distinguer les événements.
# ------------------------------------------------------------

CREATE OR REPLACE VIEW vue_planning_rh AS

# Congés validés
SELECT
    'CONGE'::VARCHAR(20) AS type_evenement,

    dc.id AS id_evenement,

    dc.id_employe,

    dc.date_debut::TIMESTAMP AS date_debut,

    dc.date_fin::TIMESTAMP AS date_fin,

    tc.libelle AS libelle,

    rc.id_employe_remplacant,

    rc.decision AS statut_remplacement,

    dc.statut_global AS statut

FROM demande_conge dc

JOIN type_conge tc
    ON tc.id = dc.id_type_conge

LEFT JOIN remplacement_conge rc
    ON rc.id_demande_conge = dc.id

WHERE dc.statut_global = 'VALIDEE'

UNION ALL

# Absences
SELECT
    'ABSENCE'::VARCHAR(20),

    a.id,

    a.id_employe,

    a.date_debut,

    a.date_fin,

    ta.libelle,

    NULL::INTEGER,

    NULL::VARCHAR(20),

    a.statut

FROM absence a

JOIN type_absence ta
    ON ta.id = a.id_type_absence

WHERE a.statut <> 'REFUSEE'

UNION ALL

# Missions
SELECT
    'MISSION'::VARCHAR(20),

    m.id,

    me.id_employe,

    m.date_debut::TIMESTAMP,

    m.date_fin::TIMESTAMP,

    m.objet,

    NULL::INTEGER,

    NULL::VARCHAR(20),

    m.statut

FROM mission m

JOIN mission_employe me
    ON me.id_mission = m.id

WHERE m.statut <> 'ANNULEE';


BEGIN;

-- ============================================================
-- 1. VUE : v_conge_solde_employe
--
-- ROLE :
-- Affiche le solde de conge de chaque employe pour chaque
-- type de conge.
--
-- CALCUL :
-- Le solde correspond a la somme des mouvements :
-- Acquisition  : positif
-- Consommation : negatif
-- Restitution  : positif
-- Expiration   : negatif
-- Annulation   : zero (trace, sans effet sur le solde)
--
-- INTERET :
-- Permet de consulter le nombre de jours disponibles
-- avant une nouvelle demande de conge.
--
-- IMPORTANT :
-- Les employes sans mouvement apparaissent avec un solde nul.
-- ============================================================

CREATE OR REPLACE VIEW v_conge_solde_employe AS
SELECT
    e.id AS id_employe,
    t.id AS id_type_conge,
    t.code AS code_type_conge,
    t.libelle AS type_conge,
    COALESCE(SUM(m.nombre_jours), 0) AS solde_disponible
FROM employe e
CROSS JOIN type_conge t
LEFT JOIN mouvement_conge m
    ON m.id_employe = e.id
    AND m.id_type_conge = t.id
GROUP BY
    e.id,
    t.id,
    t.code,
    t.libelle;


-- ============================================================
-- 2. VUE : v_conge_demandes
--
-- ROLE :
-- Affiche toutes les demandes de conge avec les informations
-- principales concernant chaque demande.
--
-- INFORMATIONS :
-- - Identifiant de la demande
-- - Identifiant de l'employe
-- - Type de conge
-- - Periode demandee
-- - Nombre de jours
-- - Motif
-- - Date de demande
-- - Statut global
--
-- INTERET :
-- Vue principale pour afficher la liste des demandes
-- dans l'interface RH et dans l'espace employe.
-- ============================================================

CREATE OR REPLACE VIEW v_conge_demandes AS
SELECT
    d.id AS id_demande,
    d.id_employe,
    d.id_type_conge,
    t.code AS code_type_conge,
    t.libelle AS type_conge,
    d.date_debut,
    d.date_fin,
    d.nombre_jours,
    d.motif,
    d.date_demande,
    d.statut_global,
    d.date_decision_finale
FROM demande_conge d
INNER JOIN type_conge t
    ON t.id = d.id_type_conge;


-- ============================================================
-- 3. VUE : v_conge_demandes_en_attente
--
-- ROLE :
-- Affiche uniquement les demandes qui attendent encore
-- une validation.
--
-- ETATS CONCERNES :
-- EN_ATTENTE_MANAGER
-- EN_ATTENTE_RH
--
-- INTERET :
-- Permet au Manager et au RH de retrouver les demandes
-- qui necessitent encore une intervention.
--
-- REMARQUE :
-- Cette vue ne remplace pas le controle des permissions.
-- Le backend doit filtrer les demandes selon le role.
-- ============================================================

CREATE OR REPLACE VIEW v_conge_demandes_en_attente AS
SELECT
    d.*
FROM v_conge_demandes d
WHERE d.statut_global IN (
    'EN_ATTENTE_MANAGER',
    'EN_ATTENTE_RH'
);


-- ============================================================
-- 4. VUE : v_conge_validations
--
-- ROLE :
-- Affiche l'historique des validations effectuees
-- sur les demandes de conge.
--
-- INFORMATIONS :
-- - Demande concernee
-- - Utilisateur ayant effectue la validation
-- - Ordre de validation
-- - Decision prise
-- - Commentaire
-- - Date de validation
--
-- INTERET :
-- Permet de suivre le circuit de validation et de connaitre
-- les decisions prises par le Manager et le RH.
--
-- IMPORTANT :
-- id_decision est traduit en code et libelle grace a
-- la table decision.
-- ============================================================

CREATE OR REPLACE VIEW v_conge_validations AS
SELECT
    v.id AS id_validation,
    v.id_demande_conge,
    v.id_utilisateur,
    v.ordre,
    CASE
        WHEN v.ordre = 1 THEN 'MANAGER'
        WHEN v.ordre = 2 THEN 'RH'
        ELSE 'INCONNU'
    END AS etape_validation,
    v.id_decision,
    dec.code AS code_decision,
    dec.libelle AS decision,
    v.commentaire,
    v.date_validation
FROM validation_conge v
INNER JOIN decision dec
    ON dec.id = v.id_decision;


-- ============================================================
-- 5. VUE : v_conge_historique_mouvements
--
-- ROLE :
-- Affiche les mouvements de solde et les traces d'annulation
-- d'un employe.
--
-- TYPES DE MOUVEMENTS :
-- ACQUISITION
-- CONSOMMATION
-- AJUSTEMENT
-- RESTITUTION
-- EXPIRATION
-- ANNULATION (nombre_jours = 0, trace sans impact sur le solde)
--
-- INFORMATIONS :
-- - Employe concerne
-- - Type de conge
-- - Demande associee si elle existe
-- - Type de mouvement
-- - Nombre de jours
-- - Date du mouvement
-- - Periode d'acquisition
-- - Motif
--
-- INTERET :
-- Permet de consulter l'historique complet du solde
-- et de justifier chaque variation.
-- ============================================================

CREATE OR REPLACE VIEW v_conge_historique_mouvements AS
SELECT
    m.id AS id_mouvement,
    m.id_employe,
    m.id_type_conge,
    t.code AS code_type_conge,
    t.libelle AS type_conge,
    m.id_demande_conge,
    m.id_utilisateur,
    m.date_mouvement,
    m.type_mouvement,
    m.nombre_jours,
    m.periode_debut,
    m.motif
FROM mouvement_conge m
INNER JOIN type_conge t
    ON t.id = m.id_type_conge;


-- ============================================================
-- 6. VUE : v_conge_calendrier
--
-- ROLE :
-- Affiche les demandes de conge definitivement validees.
--
-- INFORMATIONS :
-- - Employe concerne
-- - Type de conge
-- - Date de debut
-- - Date de fin
-- - Nombre de jours
--
-- INTERET :
-- Permet de construire le calendrier des conges dans
-- l'interface RH.
--
-- IMPORTANT :
-- Seules les demandes ayant le statut VALIDEE apparaissent.
-- Les demandes en attente ou refusees sont exclues.
-- ============================================================

CREATE OR REPLACE VIEW v_conge_calendrier AS
SELECT
    d.id AS id_demande,
    d.id_employe,
    d.id_type_conge,
    t.code AS code_type_conge,
    t.libelle AS type_conge,
    d.date_debut,
    d.date_fin,
    d.nombre_jours,
    d.statut_global
FROM demande_conge d
INNER JOIN type_conge t
    ON t.id = d.id_type_conge
WHERE d.statut_global = 'VALIDEE';


COMMIT;

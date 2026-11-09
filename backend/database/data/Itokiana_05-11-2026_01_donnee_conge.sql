BEGIN;

-- ============================================================
-- 1. TYPES DE CONGE
-- ============================================================

INSERT INTO type_conge (id, code, libelle, description, actif)
VALUES
(1, 'ANNUEL', 'Conge annuel', 'Conge annuel acquis progressivement.', TRUE),
(2, 'MALADIE', 'Conge maladie', 'Conge accorde pour raison de sante.', TRUE),
(3, 'EXCEPTIONNEL', 'Conge exceptionnel', 'Conge accorde pour un evenement particulier.', TRUE),
(4, 'SANS_SOLDE', 'Conge sans solde', 'Conge sans remuneration.', TRUE)
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- 2. REGLES DE CONGE
-- ============================================================

INSERT INTO regle_conge (
    id_type_conge,
    deduction_solde,
    justificatif_obligatoire,
    remplacant_obligatoire,
    acquisition_autorisee,
    jours_acquis_mois,
    plafond_jours
)
SELECT
    t.id,
    r.deduction_solde,
    r.justificatif_obligatoire,
    r.remplacant_obligatoire,
    r.acquisition_autorisee,
    r.jours_acquis_mois,
    r.plafond_jours
FROM (
    VALUES
    ('ANNUEL', TRUE, FALSE, TRUE, TRUE, 2.5::NUMERIC, 90::NUMERIC),
    ('MALADIE', FALSE, TRUE, FALSE, FALSE, 0::NUMERIC, NULL::NUMERIC),
    ('EXCEPTIONNEL', FALSE, FALSE, FALSE, FALSE, 0::NUMERIC, NULL::NUMERIC),
    ('SANS_SOLDE', FALSE, FALSE, FALSE, FALSE, 0::NUMERIC, NULL::NUMERIC)
) AS r(
    code,
    deduction_solde,
    justificatif_obligatoire,
    remplacant_obligatoire,
    acquisition_autorisee,
    jours_acquis_mois,
    plafond_jours
)
JOIN type_conge t ON t.code = r.code
WHERE NOT EXISTS (
    SELECT 1
    FROM regle_conge rc
    WHERE rc.id_type_conge = t.id
      AND rc.actif = TRUE
);


-- ============================================================
-- 3. JOURS FERIES
-- ============================================================

INSERT INTO jour_ferie (date_jour, libelle, description)
VALUES
('2026-01-01', 'Jour de l''An', 'Nouvel an'),
('2026-03-29', 'Commemoration', 'Jour commemoratif'),
('2026-05-01', 'Fete du Travail', 'Jour ferie national'),
('2026-06-26', 'Fete de l''Independance', 'Fete nationale'),
('2026-12-25', 'Noel', 'Jour ferie');


-- ============================================================
-- 4. DEMANDES DE CONGE
-- ============================================================

INSERT INTO demande_conge (
    id,
    id_employe,
    id_type_conge,
    date_debut,
    date_fin,
    nombre_jours,
    motif,
    date_demande,
    statut_global
)
VALUES
(1, 1, 1, '2026-11-10', '2026-11-14', 5,
 'Repos annuel', '2026-10-20', 'VALIDEE'),

(2, 2, 2, '2026-11-05', '2026-11-07', 3,
 'Probleme de sante', '2026-11-04', 'VALIDEE'),

(3, 3, 3, '2026-11-12', '2026-11-13', 2,
 'Evenement familial', '2026-10-25', 'REFUSEE'),

(4, 4, 4, '2026-12-01', '2026-12-05', 5,
 'Raison personnelle', '2026-10-28', 'EN_ATTENTE_RH'),

(5, 5, 1, '2026-12-15', '2026-12-19', 5,
 'Repos de fin d''annee', '2026-11-01', 'EN_ATTENTE_RH');


-- ============================================================
-- 5. VALIDATIONS DES DEMANDES
-- ============================================================

-- Ordre 1 : Manager
-- Ordre 2 : RH

INSERT INTO validation_conge (
    id,
    id_demande_conge,
    id_utilisateur,
    ordre,
    id_decision,
    commentaire,
    date_validation
)
VALUES
(1, 1, 2, 1, 2, 'Demande acceptee par le manager', '2026-10-20'),
(2, 1, 2, 2, 2, 'Demande acceptee par le RH', '2026-10-21'),

(3, 2, 2, 1, 2, 'Demande acceptee par le manager', '2026-11-04'),
(4, 2, 2, 2, 2, 'Certificat medical recu', '2026-11-04'),

(5, 3, 2, 1, 2, 'Demande acceptee par le manager', '2026-10-25'),
(6, 3, 2, 2, 3, 'Effectif insuffisant pendant cette periode', '2026-10-26'),

(7, 4, 2, 1, 2, 'Demande acceptee par le manager', '2026-10-28'),
(8, 4, 2, 2, 1, 'Demande en attente de validation RH', NULL),

(9, 5, 2, 1, 2, 'Demande acceptee par le manager', '2026-11-01'),
(10, 5, 2, 2, 1, 'En attente de validation RH', NULL);


-- ============================================================
-- 6. MOUVEMENTS DE CONGE
-- ============================================================

INSERT INTO mouvement_conge (
    id,
    id_employe,
    id_type_conge,
    id_demande_conge,
    id_utilisateur,
    date_mouvement,
    type_mouvement,
    nombre_jours,
    periode_debut,
    motif
)
VALUES
(1, 1, 1, NULL, 2, '2026-10-01', 'ACQUISITION', 2.5,
 '2026-10-01', 'Acquisition mensuelle octobre'),

(2, 1, 1, 1, 2, '2026-10-21', 'CONSOMMATION', -5,
 NULL, 'Consommation suite a validation du conge annuel'),

(3, 2, 1, NULL, 2, '2026-10-01', 'ACQUISITION', 2.5,
 '2026-10-01', 'Acquisition mensuelle octobre'),

(4, 3, 1, NULL, 2, '2026-10-01', 'ACQUISITION', 2.5,
 '2026-10-01', 'Acquisition mensuelle octobre'),

(5, 5, 1, NULL, 2, '2026-11-01', 'ACQUISITION', 2.5,
 '2026-11-01', 'Acquisition mensuelle novembre');


-- ============================================================
-- 7. SYNCHRONISATION DES SEQUENCES SERIAL
-- ============================================================

SELECT setval(
    pg_get_serial_sequence('type_conge', 'id'),
    COALESCE((SELECT MAX(id) FROM type_conge), 1)
);

SELECT setval(
    pg_get_serial_sequence('jour_ferie', 'id'),
    COALESCE((SELECT MAX(id) FROM jour_ferie), 1)
);

SELECT setval(
    pg_get_serial_sequence('demande_conge', 'id'),
    COALESCE((SELECT MAX(id) FROM demande_conge), 1)
);

SELECT setval(
    pg_get_serial_sequence('validation_conge', 'id'),
    COALESCE((SELECT MAX(id) FROM validation_conge), 1)
);

SELECT setval(
    pg_get_serial_sequence('mouvement_conge', 'id'),
    COALESCE((SELECT MAX(id) FROM mouvement_conge), 1)
);

COMMIT;
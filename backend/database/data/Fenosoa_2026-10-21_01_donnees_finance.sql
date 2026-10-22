-- ============================================================
-- Fenosoa - 2026-10-21 - 01
-- Raison : donnees de test pour le module Finance (grille salariale,
-- budget annuel, demandes a valider). A executer APRES :
--   1. tables/init_tables.sql
--   2. data/Doneli_20-10-2026_01_init_data.sql
--   3. data/Itokiana_21-10-2026_01_donnee_utilisateur.sql
-- A executer UNE SEULE FOIS sur une base fraiche.
-- Pour recommencer : executer le bloc "NETTOYAGE" en bas du fichier.
-- Comptes utiles : finance@exemple.mg / Test1234!
-- ============================================================

BEGIN;

-- 1. Departements (tous diriges par le compte manager de test)
INSERT INTO departement (id_utilisateur_manager, nom, effectif_max)
SELECT u.id, v.nom, v.effectif_max
FROM (VALUES
    ('Informatique', 10), ('Ressources humaines', 5), ('Finance', 5),
    ('Marketing', 6), ('Support', 6)
) AS v(nom, effectif_max)
CROSS JOIN (SELECT id FROM utilisateur WHERE email = 'manager@exemple.mg') u;

-- 2. Postes
INSERT INTO poste (id_departement, nom)
SELECT d.id, v.poste
FROM (VALUES
    ('Informatique', 'Développeur Java Spring Boot'),
    ('Informatique', 'Administrateur systèmes'),
    ('Ressources humaines', 'Chargé de recrutement'),
    ('Ressources humaines', 'Assistant RH'),
    ('Finance', 'Comptable'),
    ('Finance', 'Contrôleur de gestion'),
    ('Marketing', 'Responsable marketing digital'),
    ('Marketing', 'Community manager'),
    ('Support', 'Technicien support informatique'),
    ('Support', 'Agent d''accueil')
) AS v(departement, poste)
JOIN departement d ON d.nom = v.departement;

-- 3. Grille salariale (salaire mensuel, en ariary), en cours depuis le 01/01/2026
INSERT INTO grille_salaire (id_poste, valeur_min, valeur_max, date_debut_validite, date_fin_validite)
SELECT p.id, v.vmin, v.vmax, DATE '2026-01-01', NULL
FROM (VALUES
    ('Développeur Java Spring Boot', 1500000, 2500000),
    ('Administrateur systèmes', 1200000, 2200000),
    ('Chargé de recrutement', 1200000, 1800000),
    ('Assistant RH', 800000, 1300000),
    ('Comptable', 1000000, 1600000),
    ('Contrôleur de gestion', 1300000, 2200000),
    ('Responsable marketing digital', 1800000, 2800000),
    ('Community manager', 900000, 1600000),
    ('Technicien support informatique', 800000, 1300000),
    ('Agent d''accueil', 700000, 1200000)
) AS v(poste, vmin, vmax)
JOIN poste p ON p.nom = v.poste;

-- 4. Budget annuel 2026 par departement
INSERT INTO budget_departement (id_departement, annee, montant)
SELECT d.id, 2026, v.montant
FROM (VALUES
    ('Informatique', 250000000), ('Ressources humaines', 70000000),
    ('Finance', 60000000), ('Marketing', 90000000), ('Support', 55000000)
) AS v(departement, montant)
JOIN departement d ON d.nom = v.departement;

-- 5. Employes actifs + contrats CDI actifs (salaire mensuel)
--    Masse salariale annuelle attendue (x12) : Informatique 96 M, RH 30 M,
--    Finance 36 M, Marketing 42 M, Support 24 M.
WITH nouveaux AS (
    INSERT INTO employe (matricule, nom, prenom, email, id_departement, id_poste, date_embauche)
    SELECT v.matricule, v.nom, v.prenom, v.email, p.id_departement, p.id, DATE '2025-03-01'
    FROM (VALUES
        ('EMP-T01', 'Rabe', 'Hery', 'hery.rabe@test.mg', 'Développeur Java Spring Boot'),
        ('EMP-T02', 'Randria', 'Tiana', 'tiana.randria@test.mg', 'Développeur Java Spring Boot'),
        ('EMP-T03', 'Rakoto', 'Fanja', 'fanja.rakoto@test.mg', 'Administrateur systèmes'),
        ('EMP-T04', 'Andrian', 'Lova', 'lova.andrian@test.mg', 'Administrateur systèmes'),
        ('EMP-T05', 'Razafy', 'Noro', 'noro.razafy@test.mg', 'Chargé de recrutement'),
        ('EMP-T06', 'Ravelo', 'Soa', 'soa.ravelo@test.mg', 'Assistant RH'),
        ('EMP-T07', 'Rasolo', 'Mamy', 'mamy.rasolo@test.mg', 'Comptable'),
        ('EMP-T08', 'Rajao', 'Vola', 'vola.rajao@test.mg', 'Contrôleur de gestion'),
        ('EMP-T09', 'Ramanana', 'Kanto', 'kanto.ramanana@test.mg', 'Responsable marketing digital'),
        ('EMP-T10', 'Rakotovao', 'Aina', 'aina.rakotovao@test.mg', 'Community manager'),
        ('EMP-T11', 'Rabary', 'Toky', 'toky.rabary@test.mg', 'Technicien support informatique'),
        ('EMP-T12', 'Raharison', 'Faly', 'faly.raharison@test.mg', 'Agent d''accueil')
    ) AS v(matricule, nom, prenom, email, poste)
    JOIN poste p ON p.nom = v.poste
    RETURNING id, matricule
)
INSERT INTO contrat (reference, id_employe, id_type_contrat, id_statut_contrat, date_debut, salaire)
SELECT 'CT-' || n.matricule, n.id, tc.id, sc.id, DATE '2025-03-01', s.salaire
FROM nouveaux n
JOIN (VALUES
    ('EMP-T01', 2500000), ('EMP-T02', 2000000), ('EMP-T03', 2000000), ('EMP-T04', 1500000),
    ('EMP-T05', 1500000), ('EMP-T06', 1000000), ('EMP-T07', 1500000), ('EMP-T08', 1500000),
    ('EMP-T09', 2000000), ('EMP-T10', 1500000), ('EMP-T11', 1000000), ('EMP-T12', 1000000)
) AS s(matricule, salaire) ON s.matricule = n.matricule
CROSS JOIN (SELECT id FROM type_contrat WHERE code = 'CDI') tc
CROSS JOIN (SELECT id FROM statut_contract WHERE code = 'ACTIF') sc;

-- 6. Depenses internes 2026 (hors salaires)
--    Total attendu : Informatique 34 M, RH 8 M, Finance 12 M, Marketing 15 M, Support 6 M.
INSERT INTO depense_departement (id_departement, libelle, categorie, montant, date_depense)
SELECT d.id, v.libelle, v.categorie, v.montant, v.date_depense::date
FROM (VALUES
    ('Informatique', 'Licences logicielles', 'Logiciel', 20000000, '2026-02-10'),
    ('Informatique', 'Serveurs et materiel', 'Materiel', 14000000, '2026-04-15'),
    ('Ressources humaines', 'Formation du personnel', 'Formation', 8000000, '2026-03-20'),
    ('Finance', 'Audit externe', 'Prestation', 12000000, '2026-05-05'),
    ('Marketing', 'Campagne publicitaire', 'Communication', 15000000, '2026-06-01'),
    ('Support', 'Outils de ticketing', 'Logiciel', 6000000, '2026-02-25')
) AS v(departement, libelle, categorie, montant, date_depense)
JOIN departement d ON d.nom = v.departement;

-- 7. Demandes de recrutement (besoins) a traiter
INSERT INTO besoin (id_type_besoin, id_poste, id_utilisateur_demandeur, effectif, priorite,
                    date_arrivee_souhaitee, lieu_travail, motif, salaire_estime, statut_global)
SELECT tb.id, p.id, u.id, v.effectif, 'NORMALE', DATE '2026-12-01', 'Antananarivo',
       '[TEST FINANCE] ' || v.motif, v.salaire_estime, v.statut
FROM (VALUES
    ('Développeur Java Spring Boot', 2, 1800000, 'Renfort de l''equipe projet', 'EN_ATTENTE_FINANCE'),
    ('Comptable', 1, 1200000, 'Nouveau poste de comptabilite', 'EN_ATTENTE_FINANCE'),
    ('Chargé de recrutement', 1, 1500000, 'Hausse du volume de recrutement', 'EN_ATTENTE_RH'),
    ('Responsable marketing digital', 1, 2200000, 'Lancement d''une nouvelle gamme', 'EN_ATTENTE_DIRECTION'),
    ('Technicien support informatique', 1, 1000000, 'Remplacement d''un depart', 'REFUSE'),
    ('Community manager', 1, 1200000, 'Animation des reseaux sociaux', 'REFUSE')
) AS v(poste, effectif, salaire_estime, motif, statut)
JOIN poste p ON p.nom = v.poste
CROSS JOIN (SELECT id FROM type_besoin WHERE code = 'RENFORT') tb
CROSS JOIN (SELECT id FROM utilisateur WHERE email = 'manager@exemple.mg') u;

-- 8. Les 3 validations (RH -> FINANCE -> DIRECTION) de chaque demande
INSERT INTO validation_besoin (id_besoin, id_utilisateur_validateur, id_etat_validation, id_decision,
                               date_decision, remarque)
SELECT b.id,
       uv.id,
       ev.id,
       dc.id,
       CASE WHEN v.decision = 'EN_ATTENTE' THEN NULL ELSE now() - interval '2 days' END,
       v.remarque
FROM (VALUES
    -- Developpeur Java : RH ok, Finance a traiter
    ('Développeur Java Spring Boot', 'RH', 'ACCEPTE', 'rh@exemple.mg', NULL),
    ('Développeur Java Spring Boot', 'FINANCE', 'EN_ATTENTE', NULL, NULL),
    ('Développeur Java Spring Boot', 'DIRECTION', 'EN_ATTENTE', NULL, NULL),
    -- Comptable : RH ok, Finance a traiter (budget Finance insuffisant)
    ('Comptable', 'RH', 'ACCEPTE', 'rh@exemple.mg', NULL),
    ('Comptable', 'FINANCE', 'EN_ATTENTE', NULL, NULL),
    ('Comptable', 'DIRECTION', 'EN_ATTENTE', NULL, NULL),
    -- Charge de recrutement : RH n'a pas encore decide
    ('Chargé de recrutement', 'RH', 'EN_ATTENTE', NULL, NULL),
    ('Chargé de recrutement', 'FINANCE', 'EN_ATTENTE', NULL, NULL),
    ('Chargé de recrutement', 'DIRECTION', 'EN_ATTENTE', NULL, NULL),
    -- Responsable marketing : RH ok, Finance ok, Direction a traiter
    ('Responsable marketing digital', 'RH', 'ACCEPTE', 'rh@exemple.mg', NULL),
    ('Responsable marketing digital', 'FINANCE', 'ACCEPTE', 'finance@exemple.mg', NULL),
    ('Responsable marketing digital', 'DIRECTION', 'EN_ATTENTE', NULL, NULL),
    -- Technicien support : refuse par les RH
    ('Technicien support informatique', 'RH', 'REFUSE', 'rh@exemple.mg', 'Poste non prioritaire'),
    ('Technicien support informatique', 'FINANCE', 'EN_ATTENTE', NULL, NULL),
    ('Technicien support informatique', 'DIRECTION', 'EN_ATTENTE', NULL, NULL),
    -- Community manager : refuse par Finance, Direction refusee automatiquement (cascade)
    ('Community manager', 'RH', 'ACCEPTE', 'rh@exemple.mg', NULL),
    ('Community manager', 'FINANCE', 'REFUSE', 'finance@exemple.mg', 'Budget insuffisant'),
    ('Community manager', 'DIRECTION', 'REFUSE', 'finance@exemple.mg', 'Refus automatique suite au refus de Finance')
) AS v(poste, etape, decision, validateur_email, remarque)
JOIN poste p ON p.nom = v.poste
JOIN besoin b ON b.id_poste = p.id AND b.motif LIKE '[TEST FINANCE]%'
JOIN etat_validation ev ON ev.code = v.etape
JOIN decision dc ON dc.code = v.decision
LEFT JOIN utilisateur uv ON uv.email = v.validateur_email;

COMMIT;

-- ============================================================
-- NETTOYAGE (a decommenter pour supprimer ces donnees de test)
-- ============================================================
-- BEGIN;
-- DELETE FROM depense_departement WHERE id_departement IN (SELECT id FROM departement WHERE nom IN ('Informatique','Ressources humaines','Finance','Marketing','Support'));
-- DELETE FROM contrat WHERE reference LIKE 'CT-EMP-T%';
-- DELETE FROM employe WHERE matricule LIKE 'EMP-T%';
-- DELETE FROM besoin WHERE motif LIKE '[TEST FINANCE]%';
-- DELETE FROM grille_salaire WHERE id_poste IN (SELECT id FROM poste WHERE id_departement IN (SELECT id FROM departement WHERE nom IN ('Informatique','Ressources humaines','Finance','Marketing','Support')));
-- DELETE FROM budget_departement WHERE id_departement IN (SELECT id FROM departement WHERE nom IN ('Informatique','Ressources humaines','Finance','Marketing','Support'));
-- DELETE FROM poste WHERE id_departement IN (SELECT id FROM departement WHERE nom IN ('Informatique','Ressources humaines','Finance','Marketing','Support'));
-- DELETE FROM departement WHERE nom IN ('Informatique','Ressources humaines','Finance','Marketing','Support');
-- COMMIT;

-- Donnees de demonstration pour tester l'organigramme et sa pagination.
-- A executer apres les migrations et le jeu de donnees de base.
-- Ce script est relancable : les employes de test sont identifies par matricule.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM departement) THEN
        RAISE EXCEPTION 'Aucun departement : chargez d''abord les donnees de reference.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM poste p JOIN departement d ON d.id = p.id_departement) THEN
        RAISE EXCEPTION 'Aucun poste : chargez d''abord les donnees de reference.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM employe
        WHERE matricule = 'MAT-00295' AND statut <> 'PARTI'
    ) THEN
        RAISE EXCEPTION 'Le salarie MAT-00295 est absent ou parti : chargez les donnees de test completes.';
    END IF;
END;
$$;

INSERT INTO employe (
    matricule, nom, prenom, email, id_departement, id_poste,
    date_embauche, statut, date_creation
)
SELECT v.matricule,
       'Test Organigramme',
       v.prenom,
       lower(v.matricule) || '@exemple.test',
       cible.id_departement,
       cible.id_poste,
       CURRENT_DATE - 180,
       'ACTIF',
       CURRENT_TIMESTAMP
FROM (VALUES
    ('ORG-HARENA-001', 'Harena'),
    ('ORG-HARENA-002', 'Equipe 02'),
    ('ORG-HARENA-003', 'Equipe 03'),
    ('ORG-HARENA-004', 'Equipe 04'),
    ('ORG-HARENA-005', 'Equipe 05'),
    ('ORG-HARENA-006', 'Equipe 06'),
    ('ORG-HARENA-007', 'Equipe 07'),
    ('ORG-HARENA-008', 'Equipe 08'),
    ('ORG-HARENA-009', 'Equipe 09'),
    ('ORG-HARENA-010', 'Equipe 10'),
    ('ORG-HARENA-011', 'Equipe 11'),
    ('ORG-HARENA-012', 'Equipe 12'),
    ('ORG-HARENA-013', 'Equipe 13'),
    ('ORG-HARENA-014', 'Equipe 14'),
    ('ORG-HARENA-015', 'Equipe 15'),
    ('ORG-HARENA-016', 'Equipe 16'),
    ('ORG-HARENA-017', 'Equipe 17'),
    ('ORG-HARENA-018', 'Equipe 18'),
    ('ORG-HARENA-019', 'Equipe 19'),
    ('ORG-HARENA-020', 'Equipe 20'),
    ('ORG-HARENA-021', 'Equipe 21'),
    ('ORG-HARENA-022', 'Equipe 22'),
    ('ORG-HARENA-023', 'Equipe 23'),
    ('ORG-HARENA-024', 'Equipe 24'),
    ('ORG-HARENA-025', 'Equipe 25')
) AS v(matricule, prenom)
CROSS JOIN LATERAL (
    SELECT d.id AS id_departement, p.id AS id_poste
    FROM departement d
    JOIN LATERAL (
        SELECT poste.id
        FROM poste
        WHERE poste.id_departement = d.id
        ORDER BY poste.id
        LIMIT 1
    ) p ON TRUE
    ORDER BY d.id
    LIMIT 1
) cible
ON CONFLICT (matricule) DO NOTHING;

-- Construire une hierarchie d'exemple autour de Rivo Andriamihaja (MAT-00295).
-- Harena est son superieur, 12 salaries lui rapportent directement,
-- et 12 autres sont rattaches a l'un de ses subordonnes.
UPDATE historique_affectation h
SET id_superieur = NULL
FROM employe e
WHERE h.id_employe = e.id
  AND h.date_fin IS NULL
  AND e.matricule = 'ORG-HARENA-001';

UPDATE historique_affectation h
SET id_superieur = racine.id
FROM employe e
JOIN employe racine ON racine.matricule = 'ORG-HARENA-001'
WHERE h.id_employe = e.id
  AND h.date_fin IS NULL
  AND e.matricule = 'MAT-00295';

UPDATE historique_affectation h
SET id_superieur = responsable.id
FROM employe e
JOIN employe responsable ON responsable.matricule = 'MAT-00295'
WHERE h.id_employe = e.id
  AND h.date_fin IS NULL
  AND e.matricule BETWEEN 'ORG-HARENA-002' AND 'ORG-HARENA-013';

UPDATE historique_affectation h
SET id_superieur = responsable.id
FROM employe e
JOIN employe responsable ON responsable.matricule = 'ORG-HARENA-002'
WHERE h.id_employe = e.id
  AND h.date_fin IS NULL
  AND e.matricule BETWEEN 'ORG-HARENA-014' AND 'ORG-HARENA-025';

COMMIT;

-- La fiche MAT-00295 affiche ainsi son superieur direct, sa chaine,
-- 12 subordonnes directs et 12 subordonnes indirects.

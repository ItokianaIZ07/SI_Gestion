BEGIN;

-- Jeu de test pour v_variables_paie_mensuelles et l'ecran Variables de paie.
-- Les donnees sont ajoutees au mois precedent et les lignes deja presentes restent intactes.
WITH periode AS (
    SELECT (date_trunc('month', CURRENT_DATE)::date - INTERVAL '1 month')::date AS premier_jour
),
jours AS (
    SELECT premier_jour + decalage AS date_travail, decalage
    FROM periode
    CROSS JOIN generate_series(4, 6) AS decalage
),
employes AS (
    SELECT e.id, ROW_NUMBER() OVER (ORDER BY e.id) AS rang
    FROM employe e
    WHERE e.statut = 'ACTIF'
      AND NOT EXISTS (
          SELECT 1
          FROM temps_travail_journalier t
          CROSS JOIN jours j
          WHERE t.id_employe = e.id
            AND t.date_travail = j.date_travail
      )
    ORDER BY e.id
    LIMIT 3
),
validateur AS (
    SELECT id
    FROM utilisateur
    ORDER BY id
    LIMIT 1
),
donnees AS (
    SELECT e.id AS id_employe,
           j.date_travail,
           480 AS temps_prevu_minutes,
           CASE WHEN j.decalage = 5 THEN 465 ELSE 480 END AS temps_reel_minutes,
           CASE WHEN j.decalage = 5 THEN 15 ELSE 0 END AS retard_minutes,
           CASE
               WHEN e.rang = 1 AND j.decalage = 4 THEN 45
               WHEN e.rang = 1 AND j.decalage = 5 THEN 60
               WHEN e.rang = 2 AND j.decalage = 4 THEN 30
               WHEN e.rang = 2 AND j.decalage = 5 THEN 20
               ELSE 0
           END AS heures_supplementaires_minutes,
           CASE
               WHEN e.rang = 1 AND j.decalage = 4 THEN 'A_VALIDER'
               WHEN e.rang = 1 AND j.decalage = 5 THEN 'VALIDEE'
               WHEN e.rang = 2 AND j.decalage = 4 THEN 'REFUSEE'
               WHEN e.rang = 2 AND j.decalage = 5 THEN 'A_VALIDER'
               ELSE 'AUCUNE'
           END AS statut_heures_sup,
           CASE
               WHEN e.rang = 1 AND j.decalage = 5 THEN v.id
               WHEN e.rang = 2 AND j.decalage = 4 THEN v.id
               ELSE NULL
           END AS id_utilisateur_validateur
    FROM employes e
    CROSS JOIN jours j
    CROSS JOIN validateur v
)
INSERT INTO temps_travail_journalier (
    id_employe,
    date_travail,
    temps_prevu_minutes,
    temps_reel_minutes,
    retard_minutes,
    heures_supplementaires_minutes,
    statut_heures_sup,
    id_utilisateur_validateur,
    date_validation
)
SELECT id_employe,
       date_travail,
       temps_prevu_minutes,
       temps_reel_minutes,
       retard_minutes,
       heures_supplementaires_minutes,
       statut_heures_sup,
       id_utilisateur_validateur,
       CASE WHEN id_utilisateur_validateur IS NOT NULL THEN CURRENT_TIMESTAMP END
FROM donnees
ON CONFLICT (id_employe, date_travail) DO NOTHING;

COMMIT;

-- Verification du recapitulatif genere pour le mois precedent.
SELECT v.annee,
       v.mois,
       v.matricule,
       v.nom,
       v.prenom,
       v.minutes_prevues,
       v.minutes_travaillees,
       v.minutes_retard,
       v.nb_retards,
       v.minutes_sup_validees,
       v.minutes_sup_a_valider
FROM v_variables_paie_mensuelles v
WHERE (v.annee, v.mois) = (
    SELECT EXTRACT(YEAR FROM premier_jour)::INTEGER,
           EXTRACT(MONTH FROM premier_jour)::INTEGER
    FROM (SELECT (date_trunc('month', CURRENT_DATE)::date - INTERVAL '1 month')::date AS premier_jour) p
)
ORDER BY v.nom, v.prenom;

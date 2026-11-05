BEGIN;

-- Donnees de test pour la page de gestion des regimes de travail.
-- Les codes deja presents (par exemple TEMPS_PLEIN) ne sont pas remplaces.
INSERT INTO regime_travail (code, libelle, description, heures_hebdomadaires) VALUES
    ('TEMPS_PLEIN', 'Temps plein', 'Horaire hebdomadaire a temps plein.', 40),
    ('TEMPS_PARTIEL', 'Temps partiel', 'Horaire hebdomadaire reduit.', 20),
    ('TEMPS_PARTIEL_80', 'Temps partiel 80 %', 'Quatre jours de travail par semaine.', 32),
    ('TEMPS_REDUIT', 'Temps reduit', 'Horaire hebdomadaire reduit pour test.', 12),
    ('SAISONNIER', 'Travail saisonnier', 'Regime temporaire adapte a une activite saisonniere.', 35)
ON CONFLICT (code) DO NOTHING;

COMMIT;

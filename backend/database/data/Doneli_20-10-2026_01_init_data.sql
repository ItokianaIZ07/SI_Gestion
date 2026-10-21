BEGIN;

-- 12. Donnees de reference : le backend doit chercher par code, pas par id.
INSERT INTO role (code, libelle) VALUES
    ('MANAGER', 'Manager'), ('RH', 'Ressources humaines'),
    ('FINANCE', 'Finance'), ('DIRECTION', 'Direction')
ON CONFLICT (code) DO NOTHING;

INSERT INTO type_besoin (code, libelle) VALUES
    ('REMPLACEMENT', 'Remplacement d''un employe'),
    ('RENFORT', 'Renforcement de l''effectif'),
    ('CREATION', 'Creation d''un poste')
ON CONFLICT (code) DO NOTHING;

INSERT INTO etat_validation (code, libelle, ordre) VALUES
    ('RH', 'Validation des ressources humaines', 1),
    ('FINANCE', 'Validation financiere', 2),
    ('DIRECTION', 'Validation de la direction', 3)
ON CONFLICT (code) DO NOTHING;

INSERT INTO decision (code, libelle) VALUES
    ('EN_ATTENTE', 'En attente de decision'),
    ('ACCEPTE', 'Demande acceptee'),
    ('REFUSE', 'Demande refusee')
ON CONFLICT (code) DO NOTHING;

INSERT INTO statut_contract (code, libelle) VALUES
    ('BROUILLON', 'Contrat en preparation'),
    ('EN_ATTENTE_SIGNATURE', 'En attente de signature'),
    ('ACTIF', 'Contrat actif'), ('TERMINE', 'Contrat termine'),
    ('RESILIE', 'Contrat resilie')
ON CONFLICT (code) DO NOTHING;

INSERT INTO type_contrat (code, libelle, duree) VALUES
    ('CDI', 'Contrat a duree indeterminee', NULL),
    ('CDD', 'Contrat a duree determinee', 365),
    ('STAGE', 'Stage', 90)
ON CONFLICT (code) DO NOTHING;

INSERT INTO modele_tache_onboarding (code, libelle, categorie, delai_jours) VALUES
    ('SIGNATURE_CONTRAT', 'Signature du contrat', 'ADMINISTRATIF', 0),
    ('DOSSIER_ADMIN', 'Remise des pieces administratives (CIN, diplomes, RIB)', 'ADMINISTRATIF', 3),
    ('COMPTE_INFO', 'Creation des comptes et e-mail', 'INFORMATIQUE', 0),
    ('MATERIEL', 'Remise du materiel (PC, badge)', 'INFORMATIQUE', 0),
    ('ACCUEIL', 'Presentation de l''equipe et visite', 'ACCUEIL', 0),
    ('FORMATION_INIT', 'Formation d''integration', 'FORMATION', 7),
    ('BILAN_ESSAI', 'Point de fin de periode d''essai', 'AUTRE', 30)
ON CONFLICT (code) DO NOTHING;

COMMIT;

-- ============================================================
-- RAPPEL DU PROCESSUS METIER A IMPLEMENTER DANS LE BACKEND
-- 1. Creation du besoin et des trois validations EN_ATTENTE.
-- 2. Validation sequentielle RH -> FINANCE -> DIRECTION.
-- 3. Creation du recrutement seulement si besoin ACCEPTE.
-- 4. Creation de l'offre puis des publications.
-- 5. Candidat -> CV -> candidature -> entretiens -> decision.
-- 6. Verifier dans une transaction que RETENU ne depasse pas effectif.
-- 7. Creer employe, contrat, historique, onboarding et taches.
-- 8. Verifier le budget, l'effectif maximal et la grille salariale
--    dans le backend, avec verrouillage transactionnel si necessaire.
--
-- SECURITE : mdp doit contenir un hash BCrypt/Argon2, jamais le mot
-- de passe brut. Restreindre les acces aux CIN, RIB, certificats
-- medicaux, CV et contrats. Le DROP TABLE initial est destructif.
-- ============================================================

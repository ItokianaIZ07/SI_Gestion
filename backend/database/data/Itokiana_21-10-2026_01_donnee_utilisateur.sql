-- ============================================================
-- SIG RH - Utilisateurs de test (un compte par role)
-- A executer APRES init_tables.sql (les roles doivent exister).
-- Mot de passe de test pour tous les comptes : Test1234!
-- A CHANGER avant toute mise en ligne.
-- ============================================================

-- Necessaire pour hacher les mots de passe en BCrypt (compatible Spring Security).
-- Si l'utilisateur courant n'a pas le droit, executer cette ligne en tant que postgres.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO utilisateur (id_role, nom, prenom, email, mdp)
SELECT r.id, v.nom, v.prenom, v.email, crypt('Test1234!', gen_salt('bf', 10))
FROM (VALUES
    ('MANAGER',   'Rakoto', 'Jean',  'manager@exemple.mg'),
    ('RH',        'Lanto',  'Mialy', 'rh@exemple.mg'),
    ('FINANCE',   'Rasoa',  'Hanta', 'finance@exemple.mg'),
    ('DIRECTION', 'Andry',  'Tojo',  'direction@exemple.mg')
) AS v(role_code, nom, prenom, email)
JOIN role r ON r.code = v.role_code
ON CONFLICT (email) DO NOTHING;

-- Verification : doit afficher 4 lignes, avec un hash commencant par $2a$
SELECT u.id, u.email, r.code AS role, u.actif, left(u.mdp, 4) AS debut_hash
FROM utilisateur u
JOIN role r ON r.id = u.id_role
ORDER BY u.id;

-- ------------------------------------------------------------
-- Ajouter un autre utilisateur (exemple : un 2e manager)
-- ------------------------------------------------------------
-- INSERT INTO utilisateur (id_role, nom, prenom, email, mdp)
-- SELECT id, 'Rabe', 'Soa', 'manager2@exemple.mg', crypt('Test1234!', gen_salt('bf', 10))
-- FROM role WHERE code = 'MANAGER';

-- Changer le mot de passe d'un utilisateur
-- UPDATE utilisateur SET mdp = crypt('NouveauMotDePasse!', gen_salt('bf', 10))
-- WHERE email = 'rh@exemple.mg';

-- Desactiver un compte (la connexion sera refusee)
-- UPDATE utilisateur SET actif = FALSE WHERE email = 'rh@exemple.mg';
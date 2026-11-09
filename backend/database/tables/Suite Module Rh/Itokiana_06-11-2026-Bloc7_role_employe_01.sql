-- BLOC 7 : ROLE D'AUTHENTIFICATION DES EMPLOYES
-- A executer apres init_tables.sql.

BEGIN;

INSERT INTO role (code, libelle)
VALUES ('EMPLOYE', 'Employé')
ON CONFLICT (code) DO NOTHING;

-- Validation bloquante : garantit que le role a bien ete ajoute.
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM role WHERE code = 'EMPLOYE') <> 1 THEN
        RAISE EXCEPTION 'Le role EMPLOYE est absent ou duplique.';
    END IF;
END
$$;

SELECT id, code, libelle FROM role WHERE code = 'EMPLOYE';

COMMIT;

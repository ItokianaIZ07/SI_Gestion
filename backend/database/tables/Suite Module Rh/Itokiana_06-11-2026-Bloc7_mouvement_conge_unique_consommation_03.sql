-- Garantie d'une seule consommation par demande de conge.
-- A executer apres la creation de mouvement_conge.

BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM mouvement_conge
        WHERE type_mouvement = 'CONSOMMATION'
          AND id_demande_conge IS NOT NULL
        GROUP BY id_demande_conge
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Migration annulee : plusieurs mouvements CONSOMMATION existent deja pour une demande. Corriger les doublons avant de relancer.';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_mouvement_conso_demande
    ON mouvement_conge (id_demande_conge)
    WHERE type_mouvement = 'CONSOMMATION'
      AND id_demande_conge IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_class tbl ON tbl.oid = i.indrelid
        WHERE idx.relname = 'uq_mouvement_conso_demande'
          AND tbl.relname = 'mouvement_conge'
          AND i.indisunique
          AND i.indisvalid
    ) THEN
        RAISE EXCEPTION
            'Validation echouee : index unique uq_mouvement_conso_demande absent ou invalide.';
    END IF;
END
$$;

COMMIT;

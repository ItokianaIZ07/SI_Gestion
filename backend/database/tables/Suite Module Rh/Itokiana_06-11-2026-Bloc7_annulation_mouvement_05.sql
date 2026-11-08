-- Autorise un mouvement ANNULATION sans impact sur le solde et garantit
-- une seule trace d'annulation par demande.
-- Ne cree aucune table.

BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM mouvement_conge
        WHERE type_mouvement = 'ANNULATION'
          AND id_demande_conge IS NOT NULL
        GROUP BY id_demande_conge
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Migration annulee : plusieurs mouvements ANNULATION existent pour une demande.';
    END IF;
END
$$;

ALTER TABLE mouvement_conge
    DROP CONSTRAINT IF EXISTS ck_mouvement_type,
    DROP CONSTRAINT IF EXISTS ck_mouvement_nombre,
    DROP CONSTRAINT IF EXISTS ck_mouvement_signe;

ALTER TABLE mouvement_conge
    ADD CONSTRAINT ck_mouvement_type
        CHECK (
            type_mouvement IN (
                'ACQUISITION',
                'CONSOMMATION',
                'AJUSTEMENT',
                'RESTITUTION',
                'EXPIRATION',
                'ANNULATION'
            )
        ),
    ADD CONSTRAINT ck_mouvement_nombre
        CHECK (
            (type_mouvement = 'ANNULATION' AND nombre_jours = 0)
            OR (type_mouvement <> 'ANNULATION' AND nombre_jours <> 0)
        ),
    ADD CONSTRAINT ck_mouvement_signe
        CHECK (
            (type_mouvement = 'ACQUISITION' AND nombre_jours > 0)
            OR (type_mouvement = 'CONSOMMATION' AND nombre_jours < 0)
            OR (type_mouvement = 'RESTITUTION' AND nombre_jours > 0)
            OR (type_mouvement = 'AJUSTEMENT')
            OR (type_mouvement = 'ANNULATION' AND nombre_jours = 0)
            OR (type_mouvement = 'EXPIRATION' AND nombre_jours < 0)
        );

CREATE UNIQUE INDEX IF NOT EXISTS uq_mouvement_annulation_demande
    ON mouvement_conge (id_demande_conge)
    WHERE type_mouvement = 'ANNULATION'
      AND id_demande_conge IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_class tbl ON tbl.oid = i.indrelid
        WHERE idx.relname = 'uq_mouvement_annulation_demande'
          AND tbl.relname = 'mouvement_conge'
          AND i.indisunique
          AND i.indisvalid
    ) THEN
        RAISE EXCEPTION
            'Validation echouee : index unique uq_mouvement_annulation_demande absent ou invalide.';
    END IF;
END
$$;

COMMIT;

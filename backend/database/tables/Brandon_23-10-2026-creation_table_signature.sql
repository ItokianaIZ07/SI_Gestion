CREATE TABLE signature_token (
    id SERIAL PRIMARY KEY,
    id_contrat INTEGER NOT NULL REFERENCES contrat(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex du token
    email_destinataire VARCHAR(150) NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    date_expiration TIMESTAMPTZ NOT NULL,
    utilise BOOLEAN NOT NULL DEFAULT FALSE,
    date_utilisation TIMESTAMPTZ
);

CREATE INDEX idx_signature_token_hash ON signature_token(token_hash);
CREATE INDEX idx_signature_token_contrat ON signature_token(id_contrat);





-- creation vrai signature
ALTER TABLE historique_contrat ADD COLUMN signature_image BYTEA;
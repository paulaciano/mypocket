-- ADR-0005: one record per user and key (I2), inserted at the start of the transaction that moves the
-- money and completed with the response before commit. Records do not expire (I7).
CREATE TABLE idempotency_records (
    user_id         UUID         NOT NULL REFERENCES users (id),
    idempotency_key VARCHAR(100) NOT NULL,
    operation       VARCHAR(32)  NOT NULL,
    fingerprint     VARCHAR(64)  NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_idempotency_records PRIMARY KEY (user_id, idempotency_key),
    CONSTRAINT ck_idempotency_records_operation CHECK (operation IN ('TRANSFER', 'WITHDRAWAL'))
);

-- R1: money operations rejected by a business rule, kept for audit and support. They move no money and
-- are not part of the ledger. Identifiers are stored as requested, so they have no foreign keys.
CREATE TABLE rejected_operations (
    id                         UUID          PRIMARY KEY,
    type                       VARCHAR(32)   NOT NULL,
    reason                     VARCHAR(64)   NOT NULL,
    user_id                    UUID,
    provider_reference         VARCHAR(100),
    source_wallet_id           UUID,
    destination_account_number VARCHAR(10),
    currency                   VARCHAR(3)    NOT NULL,
    amount                     NUMERIC(19,2) NOT NULL,
    created_at                 TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_rejected_operations_type CHECK (type IN ('CASH_IN', 'TRANSFER', 'WITHDRAWAL')),
    CONSTRAINT ck_rejected_operations_currency CHECK (currency IN ('ARS', 'USD')),
    CONSTRAINT ck_rejected_operations_amount_positive CHECK (amount > 0)
);

CREATE INDEX ix_rejected_operations_created_at ON rejected_operations (created_at);

-- R2: append-only, like the ledger.
CREATE FUNCTION reject_audit_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit record is append-only: % on % is not allowed', TG_OP, TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER tg_rejected_operations_append_only
    BEFORE UPDATE OR DELETE ON rejected_operations
    FOR EACH ROW EXECUTE FUNCTION reject_audit_change();
CREATE TRIGGER tg_rejected_operations_no_truncate
    BEFORE TRUNCATE ON rejected_operations
    FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_change();

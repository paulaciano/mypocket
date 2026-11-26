-- System wallets (EXTERNAL, WITHDRAWALS_IN_TRANSIT) live in the same table as customer wallets.
-- They have no owner, no account number and no cached balance (L5).
ALTER TABLE wallets
    ADD COLUMN kind    VARCHAR(16) NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN purpose VARCHAR(32);
ALTER TABLE wallets ALTER COLUMN kind DROP DEFAULT;
ALTER TABLE wallets ALTER COLUMN account_number DROP NOT NULL;
ALTER TABLE wallets ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE wallets ALTER COLUMN balance DROP NOT NULL;
ALTER TABLE wallets
    ADD CONSTRAINT ck_wallets_kind CHECK (
        (kind = 'CUSTOMER' AND purpose IS NULL
            AND account_number IS NOT NULL AND owner_id IS NOT NULL AND balance IS NOT NULL)
        OR (kind = 'SYSTEM' AND purpose IN ('EXTERNAL', 'WITHDRAWALS_IN_TRANSIT')
            AND account_number IS NULL AND owner_id IS NULL AND balance IS NULL)),
    ADD CONSTRAINT uk_wallets_purpose_currency UNIQUE (purpose, currency);

INSERT INTO wallets (id, kind, purpose, currency, status, created_at)
SELECT uuidv7(), 'SYSTEM', purpose, currency, 'ACTIVE', now()
FROM (VALUES ('EXTERNAL'), ('WITHDRAWALS_IN_TRANSIT')) AS p (purpose)
CROSS JOIN (VALUES ('ARS'), ('USD')) AS c (currency);

CREATE TABLE transactions (
    id                 UUID          PRIMARY KEY,
    type               VARCHAR(32)   NOT NULL,
    currency           VARCHAR(3)    NOT NULL,
    amount             NUMERIC(19,2) NOT NULL,
    external_reference VARCHAR(100),
    created_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_transactions_type CHECK (type IN
        ('CASH_IN', 'TRANSFER', 'WITHDRAWAL', 'WITHDRAWAL_SETTLEMENT', 'WITHDRAWAL_REVERSAL')),
    CONSTRAINT ck_transactions_currency CHECK (currency IN ('ARS', 'USD')),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT uk_transactions_type_reference UNIQUE (type, external_reference)
);

CREATE TABLE ledger_entries (
    id             UUID          PRIMARY KEY,
    transaction_id UUID          NOT NULL REFERENCES transactions (id),
    wallet_id      UUID          NOT NULL REFERENCES wallets (id),
    direction      VARCHAR(6)    NOT NULL,
    amount         NUMERIC(19,2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_ledger_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0)
);

CREATE INDEX ix_ledger_entries_transaction ON ledger_entries (transaction_id);
CREATE INDEX ix_ledger_entries_wallet_history ON ledger_entries (wallet_id, created_at DESC, id DESC);

-- L2: the ledger is append-only, also for anyone writing to the database directly.
CREATE FUNCTION reject_ledger_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ledger is append-only: % on % is not allowed', TG_OP, TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER tg_transactions_append_only
    BEFORE UPDATE OR DELETE ON transactions
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_change();
CREATE TRIGGER tg_transactions_no_truncate
    BEFORE TRUNCATE ON transactions
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ledger_change();
CREATE TRIGGER tg_ledger_entries_append_only
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_change();
CREATE TRIGGER tg_ledger_entries_no_truncate
    BEFORE TRUNCATE ON ledger_entries
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ledger_change();

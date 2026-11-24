CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(trim(email))),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE wallets (
    id             UUID          PRIMARY KEY,
    account_number VARCHAR(10)   NOT NULL,
    owner_id       UUID          NOT NULL REFERENCES users (id),
    currency       VARCHAR(3)    NOT NULL,
    status         VARCHAR(16)   NOT NULL,
    balance        NUMERIC(19,2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_wallets_account_number UNIQUE (account_number),
    CONSTRAINT uk_wallets_owner_currency UNIQUE (owner_id, currency),
    CONSTRAINT ck_wallets_account_number CHECK (account_number ~ '^[1-9][0-9]{9}$'),
    CONSTRAINT ck_wallets_currency CHECK (currency IN ('ARS', 'USD')),
    CONSTRAINT ck_wallets_status CHECK (status IN ('ACTIVE', 'DEBIT_BLOCKED', 'FROZEN', 'CLOSED')),
    CONSTRAINT ck_wallets_balance_non_negative CHECK (balance >= 0)
);

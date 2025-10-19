CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    number CHAR(5) NOT NULL UNIQUE,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    value NUMERIC(15, 2) NOT NULL,
    source_account_id UUID,
    destination_account_id UUID,
    CONSTRAINT fk_source_account
        FOREIGN KEY (source_account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_destination_account
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
);
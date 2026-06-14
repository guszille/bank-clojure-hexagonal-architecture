CREATE SEQUENCE account_number_sequence START 1;

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

-- Inbox for consumer idempotency: records the outcome of each settled Transaction.requested event (keyed by event id) so a
-- redelivered event replays its decision instead of re-applying.
CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    outcome VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Transactional outbox: producers write the event to publish here, in the same transaction as the business change. A relay
-- drains unsent rows to Kafka, so the DB change and the publish can't diverge.
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255),
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX outbox_unsent_idx ON outbox (created_at) WHERE sent_at IS NULL;
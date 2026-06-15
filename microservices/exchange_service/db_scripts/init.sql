CREATE TABLE investors (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL
);

CREATE TABLE issuers (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL
);

CREATE TABLE loans (
    id UUID PRIMARY KEY,
    principal NUMERIC(15, 2) NOT NULL,
    rate NUMERIC(5, 2) NOT NULL,
    inception_date char(10) NOT NULL,
    term SMALLINT NOT NULL,
    investor_id UUID NOT NULL,
    issuer_id UUID NOT NULL,
    CONSTRAINT fk_investor
        FOREIGN KEY (investor_id)
        REFERENCES investors(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_issuer
        FOREIGN KEY (issuer_id)
        REFERENCES issuers(id)
        ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL
);

-- Transactional outbox: producers write the event to publish here, in the same transaction as the
-- business change. A relay drains unsent rows to Kafka, so the DB change and the publish can't diverge.
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255),
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX outbox_unsent_idx ON outbox (created_at) WHERE sent_at IS NULL;
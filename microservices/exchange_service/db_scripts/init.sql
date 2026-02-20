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
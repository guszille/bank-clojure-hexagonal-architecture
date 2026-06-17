# Bank — a Clojure microservices demo

A small Bank application written in **Clojure**, structured around **hexagonal architecture** and split into two **microservices** that communicate through **Kafka** events. Each service owns its own **PostgreSQL** database, and a **Nginx** reverse proxy fronts the public API.

Built with: Clojure 1.12, Finagle (HTTP), next.jdbc + HoneySQL, PostgreSQL 15, Apache Kafka (KRaft, single-node), Nginx, Docker Compose.

## Architecture

Two bounded contexts, each a microservice with its own database:

- **Ledger** — owns money: `accounts` and `transactions`.
- **Exchange** — owns financial products: `investors`, `issuers`, and `loans`.

Both services follow the same hexagonal layout under `src/bank/`:

```
domain/        # Records and pure business rules (no I/O)
application/   # Use cases that orchestrate the domain via ports
ports/         # Protocols (Repository, EventPublisher) — the interfaces
adapters/      # Concrete implementations: Finagle HTTP, Postgres, Kafka
```

### Service ports

| Component       | Container            | Host port |
|-----------------|----------------------|-----------|
| Reverse proxy   | `reverse-proxy`      | `8080`    |
| Ledger HTTP     | `ledger-service-app` | `3001`    |
| Exchange HTTP   | `exchange-service-app` | `3002`  |
| Ledger DB       | `ledger-service-db`  | `5001`    |
| Exchange DB     | `exchange-service-db`| `5002`    |
| Kafka broker    | `kafka`              | `9092`    |

The proxy routes `/api/ledger/*` to the ledger service and `/api/exchange/*` to the exchange service — see [reverse_proxy/nginx.conf](microservices/reverse_proxy/nginx.conf).

### Kafka topics

| Topic                   | Producer | Consumer | Purpose                                                  |
|-------------------------|----------|----------|----------------------------------------------------------|
| `Transaction.requested` | exchange | ledger   | Exchange asks the ledger to settle a loan as a transfer. |
| `Transaction.approved`  | ledger   | exchange | Ledger confirms the transfer happened.                   |
| `Transaction.denied`    | ledger   | exchange | Ledger rejects the transfer (e.g. insufficient funds).   |

### Loan settlement flow

```mermaid
sequenceDiagram
    participant C as Client
    participant E as Exchange
    participant K as Kafka
    participant L as Ledger

    C->>E: POST /loans
    E->>E: create loan (status = "created")
    E->>K: Transaction.requested
    E-->>C: 201 loan
    K->>L: Transaction.requested
    L->>L: debit source, credit destination,<br/>insert transaction
    L->>K: Transaction.approved / Transaction.denied
    K->>E: Transaction.approved / Transaction.denied
    E->>E: update loan status
    C->>E: GET /loans/:id
    E-->>C: status
```

The diagram shows the logical flow; the section below explains how each hop is made durable.

## Reliability & consistency

The two services coordinate over Kafka, which delivers **at least once**. To keep money from being lost, double-counted, or left half-applied, three mechanisms work together.

### Transactional operations

Every money movement runs as a single database transaction through a `with-tx` unit-of-work port: the application layer expresses *"do this atomically"*, and the Postgres adapter binds one transacted connection for the whole unit.

- **Ledger** — a deposit, withdrawal, or transfer updates balances **and** inserts the transaction row all-or-nothing. A transfer locks both account rows (`SELECT … FOR UPDATE`, in a sorted order to avoid deadlocks) and enforces `balance >= 0` in the domain, so concurrent transfers can't lose updates or overdraw.
- **Exchange** — creating a loan inserts the loan **and** enqueues its settlement event in one transaction (see the outbox below).

If any step fails, the whole unit rolls back and nothing is written.

### Idempotent consumers

Because Kafka can redeliver, a consumer may see the same event twice. Handlers are idempotent, so reprocessing is a no-op:

- **Ledger** keeps a `processed_events` **inbox**. Settlement records the event's outcome (`approved` / `denied`) in the *same transaction* as the money movement; a redelivered event replays the recorded outcome instead of moving money again (and a denial stays a denial).
- **Exchange** uses the loan's own status as the dedupe record — a settlement for a loan that is no longer `created` is skipped.

### Transactional outbox

Publishing to Kafka right after a DB commit is a dual-write: a crash in between would lose the event. Instead, a producer writes the event into an `outbox` table **in the same transaction** as the business change. A background **relay** polls unsent rows and publishes them to Kafka, marking a row sent only after the broker acknowledges — so a crash simply re-sends it.

State change and event publication are therefore atomic. Combined with idempotent consumers, the round-trip achieves an end-to-end **exactly-once effect** over at-least-once transport.

| Concern                  | Ledger                             | Exchange                |
|--------------------------|------------------------------------|-------------------------|
| Unit-of-work transaction | balances + transaction row         | loan + outbox row       |
| Idempotency record       | `processed_events` inbox           | loan `status`           |
| Outbox events            | `Transaction.approved` / `.denied` | `Transaction.requested` |

## Requirements

- Docker (with Compose v2).
- A `microservices/.env` file with these variables:

```dotenv
LEDGER_SERVICE_POSTGRES_DB=ledger
LEDGER_SERVICE_POSTGRES_USER=ledger
LEDGER_SERVICE_POSTGRES_PASSWORD=changeme

EXCHANGE_SERVICE_POSTGRES_DB=exchange
EXCHANGE_SERVICE_POSTGRES_USER=exchange
EXCHANGE_SERVICE_POSTGRES_PASSWORD=changeme
```

## Build & run

From the `microservices/` directory, with Docker running:

```bash
# day to day — cached build, keeps data:
docker compose build && docker compose up -d

# clean slate — wipes volumes so init.sql re-runs (needed after a schema change):
docker compose down -v --remove-orphans && docker compose build && docker compose up -d
```

Convenience runners wrap these. On Windows, `stack.cmd`:

```cmd
stack up      :: build (cached) and start in the background
stack reset   :: wipe data, rebuild, start
stack logs    :: tail logs (e.g. "stack logs ledger-service-app" for one service)
stack down    :: stop, keeping data
```

Or cross-platform with GNU Make (from `microservices/`): `make up`, `make reset`, `make logs` (`make logs SVC=<service>`), `make down`.

Once everything is up, the public API is at `http://localhost:8080`, and a few dashboards come up alongside it:

| Tool     | URL                     | For                                  |
|----------|-------------------------|--------------------------------------|
| Dozzle   | `http://localhost:8081` | container logs                       |
| Kafka UI | `http://localhost:8082` | topics, messages, consumer-group lag |
| Adminer  | `http://localhost:8083` | both Postgres databases              |

## API

All endpoints accept and return `application/json`. Money values are encoded as **BigDecimal-tagged strings** with a trailing `M` (e.g. `"1000.00M"`) — both on the way in and on the way out of monetary fields. UUIDs are standard string form.

### Ledger

| Method | Path                     | Body                                                                                    | Notes                                                                 |
|--------|--------------------------|-----------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| POST   | `/api/ledger/accounts`   | _(none)_                                                                                | Creates an account with a 5-digit number and balance `0.00`.          |
| GET    | `/api/ledger/accounts/:id` | _(none)_                                                                              | Returns the account.                                                  |
| POST   | `/api/ledger/transactions` | See below                                                                             | `type` ∈ {`deposit`, `withdrawal`, `transfer`}; balance updated atomically. |
| GET    | `/api/ledger/transactions/:id` | _(none)_                                                                          | Returns the transaction.                                              |

Transaction body shape:

```jsonc
// deposit
{ "type": "deposit",    "value": "100.00M", "destination-account-id": "<uuid>" }
// withdrawal
{ "type": "withdrawal", "value": "100.00M", "source-account-id": "<uuid>" }
// transfer
{ "type": "transfer",   "value": "100.00M", "source-account-id": "<uuid>", "destination-account-id": "<uuid>" }
```

### Exchange

| Method | Path                       | Body                                  | Notes                                                              |
|--------|----------------------------|---------------------------------------|--------------------------------------------------------------------|
| POST   | `/api/exchange/investors`  | `{ "account-id": "<uuid>" }`          | Links an investor to a ledger account.                             |
| GET    | `/api/exchange/investors/:id` | _(none)_                           |                                                                    |
| POST   | `/api/exchange/issuers`    | `{ "account-id": "<uuid>" }`          | Links an issuer to a ledger account.                               |
| GET    | `/api/exchange/issuers/:id` | _(none)_                             |                                                                    |
| POST   | `/api/exchange/loans`      | See below                             | Returns immediately with `status: "created"`; settlement is async. |
| GET    | `/api/exchange/loans/:id`  | _(none)_                              | Status transitions to `approved` or `denied` after Kafka round-trip. |

Loan body:

```json
{
  "principal": "100.00M",
  "rate": "10.00M",
  "inception-date": "2025-01-01",
  "term": 6,
  "investor-id": "<uuid>",
  "issuer-id": "<uuid>"
}
```

## Bootstrap script

[`microservices/scripts/requests_bootstrap.py`](microservices/scripts/requests_bootstrap.py) drives a full end-to-end happy path against a running stack:

1. Create investor and issuer accounts on **ledger**.
2. Deposit funds into the investor's account.
3. Register the investor and issuer on **exchange**.
4. Create a loan; **exchange** publishes `Transaction.requested`.
5. **Ledger** consumes the request, performs the transfer, and replies with `Transaction.approved` or `Transaction.denied`.
6. **Exchange** consumes the reply and updates the loan status.
7. The script polls the loan until it settles, then prints the final account balances.

Run it with `python microservices/scripts/requests_bootstrap.py` once the stack is up.

## Tests

Each service has its own test suite under `test/bank/` covering the domain, application, and HTTP-controller layers. Run them per-service with the Clojure CLI:

```bash
cd microservices/ledger_service   && clojure -M:test
cd microservices/exchange_service && clojure -M:test
```

Both suites also run on every push and pull request via [`.github/workflows/test.yml`](.github/workflows/test.yml).

## Next steps

- Tighten the domain layer toward DDD (aggregates, value objects, invariants on update).
- Add integration tests that exercise the full Kafka round-trip with Testcontainers.
- Document error responses and add input validation at the controller boundary.

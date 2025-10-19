# Bank

Simple bank project written in Clojure, applying hexagonal architecture, and using Finagle to set up an HTTP server.

### Monolithic Architecture [Deprecated]

For now, the application handles two domains: **accounts** and **transactions**. Also, one of the adapters manages the communications with a PostgreSQL database.

### Microservices Architecture

For now, the application handles two bounded contexts, or microservices: **ledger** and **exchange**. Each microservice has its own server and PostgreSQL database. Also, a Nginx reverse proxy is set up to receive overall requests and redirect them to the appropriate service.

#### Ledger

- Accounts; and
- Transactions.

#### Exchange

- Investors;
- Issuers; and
- Loans.

## Requirements

- Install and run Docker; and

### Monolithic

- Create a `/monolithic/.env` with the parameters:
  - POSTGRES_DB;
  - POSTGRES_USER; and
  - POSTGRES_PASSWORD.

### Microservices

- Create `/microservices/.env` with the parameters:
  - LEDGER_SERVICE_POSTGRES_DB;
  - LEDGER_SERVICE_POSTGRES_USER;
  - LEDGER_SERVICE_POSTGRES_PASSWORD;
  - EXCHANGE_SERVICE_POSTGRES_DB;
  - EXCHANGE_SERVICE_POSTGRES_USER; and
  - EXCHANGE_SERVICE_POSTGRES_PASSWORD.

## Build & Run

On Windows, execute `build.cmd` inside `/monolithic` or `/microservices` directories. Otherwise, run the commands inside it manually.
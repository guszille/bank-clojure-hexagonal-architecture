# Bank

Simple bank project written in Clojure, applying hexagonal architecture, and using Finagle to set up an HTTP server.

### Monolithic Architecture

For now, the application handles two domains: **accounts** and **transactions**. Also, one of the adapters manages the communications with a PostgreSQL database.

This implemmentation is **deprecated** since the beginning of the microservices approach development.

### Microservices Architecture

For now, the application handles two bounded contexts, or microservices: **ledger** and **exchange**. Each microservice has its own server and PostgreSQL database. A Nginx reverse proxy is set up to receive overall requests and redirect them to the appropriate service. The communication between services is done through Kafka message producers and consumers.

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

If you are executing the **microservices architecture**, you can run the `/scripts/requests_bootstrap.py` to trigger the flow of events below:

1. Request to create an investor's account.
2. Request to create an issuer's account.
3. Request to create a deposit transaction to the investor's account.
4. Request to create an investor (linked to its account).
5. Request to create an issuer (linked to its account).
6. Request to create a loan.
    * Exchange service produces a **transaction requested** message;
    * Ledger service consumes the message and evaluates whether it can create the transaction;
    * Ledger service reply with a **transaction approved** or a **transaction denied** message; and
    * Finally, Exchange service consumes the message and updates the loan status.

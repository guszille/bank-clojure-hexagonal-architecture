# Bank

A simple Bank application written in Clojure, and implementing Hexagonal Architecture.

### Monolithic Architecture

For now, the application handles two domains: **accounts** and **transactions**. Also, it has one adapter implemmenting a HTTP server with Finagle and one adapter mannaging the communication with a PostgreSQL database.

This implemmentation is **deprecated** since the beginning of the microservices architecture development.

### Microservices Architecture

For now, the application handles two bounded contexts, or microservices: **ledger** and **exchange**. Each microservice has its own Finagle adapter (HTTP server) and its own PostgreSQL adapter (database connection). Also, a Nginx reverse proxy is set up to receive overall requests and redirect them to the appropriate service. Finally, the communication between services is done through Kafka message producers and consumers.

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

1. Request to **ledger** to create an investor's account.
2. Request to **ledger** to create an issuer's account.
3. Request to **ledger** to create a deposit transaction to the investor's account.
4. Request to **exchange** to create an investor (linked to its account).
5. Request to **exchange** to create an issuer (linked to its account).
6. Request to **exchange** to create a loan.
    * The exchange service produces a **transaction requested** message;
    * The ledger service consumes the message and evaluates whether it can create the transaction;
    * The ledger service reply with a **transaction approved** or a **transaction denied** message; and
    * Finally, the exchange service consumes the message and updates the loan status.

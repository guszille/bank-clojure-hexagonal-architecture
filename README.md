# Bank

A simple Bank application written in Clojure, implementing **hexagonal architecture** and distributed in **microservices**.

### Overall Architecture

For now, the application handles two bounded contexts, expressed as microservices: **ledger** and **exchange**. Each microservice has its own Finagle adapter (HTTP server) and its own PostgreSQL adapter (database connection). Also, a Nginx reverse proxy is set up to receive all requests and redirect them to the appropriate service. Finally, the communication between services is done through Kafka messages producers and consumers.

#### Ledger Service Entities

- Accounts; and
  - `create-account`; and
  - `update-account-balance`.
- Transactions.
  - `create-transaction`.

#### Exchange Service Entities

- Investors;
  - `create-investor`.
- Issuers; and
  - `create-issuer`.
- Loans.
  - `create-loan`; and
  - `update-loan-status`.

## Requirements

- Install Docker; and
- Create an environment file in `/microservices/.env`, with the variables:
  - LEDGER_SERVICE_POSTGRES_DB;
  - LEDGER_SERVICE_POSTGRES_USER;
  - LEDGER_SERVICE_POSTGRES_PASSWORD;
  - EXCHANGE_SERVICE_POSTGRES_DB;
  - EXCHANGE_SERVICE_POSTGRES_USER; and
  - EXCHANGE_SERVICE_POSTGRES_PASSWORD.

## Build & Run

On Windows, execute `build.cmd` inside `/microservices` directory. Otherwise, run the commands inside the `.cmd` file manually.

You can also run the `/scripts/requests_bootstrap.py` to trigger the flow of events below:

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

## Next Steps

- Adjust domain and application (services) according to the DDD specifications; and
- Create automated unit and integration test cases.

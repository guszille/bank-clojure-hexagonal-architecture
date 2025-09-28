# Bank

Simple bank project written in Clojure, applying hexagonal architecture, and using Finagle to setup an HTTP server.

For now, the applications handles two domains: **accounts** and **transactions**. Also, one of the adapters manage the communications with a PostgreSQL database.

## Requirements

- Install and run Docker; and
- Configure a `.env` file with `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.

## Build & Run

On Windows, execute `build.cmd`. Otherwise, run the commands inside it manually.
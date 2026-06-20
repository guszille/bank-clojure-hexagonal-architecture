(ns bank-ui.config)

;; Relative API roots. The SPA is served same-origin behind the reverse proxy, which routes these
;; prefixes to the ledger / exchange services -- so no host, port, or CORS handling is needed here.
(def ledger-base "/api/ledger")
(def exchange-base "/api/exchange")

;; How often to re-poll a freshly created loan until it settles (created -> approved/denied).
(def loan-poll-ms 1500)

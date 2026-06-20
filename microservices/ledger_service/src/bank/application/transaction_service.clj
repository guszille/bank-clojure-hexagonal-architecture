(ns bank.application.transaction-service
    (:require [bank.domain.transaction :as domain]
              [bank.application.account-service :as account-service]
              [bank.ports.repository :as ports]
              [bank.ports.event-publisher :as event-ports]
    )
)

(defn- perform-transfer! [tx value source-account-id destination-account-id]
    ;; Core transfer mechanics. Must run inside a (ports/with-tx ...) body: locks both account rows in a deterministic (sorted)
    ;; order so opposing transfers can't deadlock, applies the debit/credit through the domain (which enforces balance >= 0),
    ;; and inserts the transaction.
    (when (= source-account-id destination-account-id)
        (throw (ex-info "Failed to transfer, source and destination accounts must differ!" {:source-account-id source-account-id :destination-account-id destination-account-id}))
    )
    (doseq [id (sort [source-account-id destination-account-id])]
        (ports/get-for-update tx :accounts id)
    )
    (account-service/apply-balance-delta! tx source-account-id (- value))
    (account-service/apply-balance-delta! tx destination-account-id value)
    (let [new-transaction (domain/create-transaction (java.util.UUID/randomUUID) "transfer" value source-account-id destination-account-id)]
        (ports/insert! tx :transactions new-transaction)
        new-transaction
    )
)

(defn deposit! [repository value destination-account-id]
    (ports/with-tx repository
        (fn [tx]
            (account-service/apply-balance-delta! tx destination-account-id value)
            (let [new-transaction (domain/create-transaction (java.util.UUID/randomUUID) "deposit" value nil destination-account-id)]
                (ports/insert! tx :transactions new-transaction)
                new-transaction
            )
        )
    )
)

(defn withdraw! [repository value source-account-id]
    (ports/with-tx repository
        (fn [tx]
            (account-service/apply-balance-delta! tx source-account-id (- value))
            (let [new-transaction (domain/create-transaction (java.util.UUID/randomUUID) "withdrawal" value source-account-id nil)]
                (ports/insert! tx :transactions new-transaction)
                new-transaction
            )
        )
    )
)

(defn transfer! [repository value source-account-id destination-account-id]
    (ports/with-tx repository
        (fn [tx]
            (perform-transfer! tx value source-account-id destination-account-id)
        )
    )
)

(defn settle-requested-transfer! [repository event-publisher event-id value source-account-id destination-account-id]
    ;; Idempotent settlement of a Transaction.requested event. On first delivery the money movement, the processed_events inbox
    ;; row, and the outbound reply event (written to the outbox) all commit in one transaction -- so the reply can't be lost,
    ;; nor published without the money having moved. A redelivered event replays the recorded decision (the relay still
    ;; publishes the original outbox row at-least-once). Returns "approved" or "denied".
    (if-let [recorded (ports/get-by-id repository :processed-events event-id)]
        (get recorded :outcome)
        (try
            (ports/with-tx repository
                (fn [tx]
                    (perform-transfer! tx value source-account-id destination-account-id)
                    (ports/insert! tx :processed-events {:id event-id :outcome "approved"})
                    (event-ports/enqueue-transaction-approved! event-publisher tx event-id)
                )
            )
            "approved"
            (catch Exception e
                (println "Ledger settlement denied for event" (str event-id) "-" (.getMessage e))
                (ports/with-tx repository
                    (fn [tx]
                        (ports/insert! tx :processed-events {:id event-id :outcome "denied"})
                        (event-ports/enqueue-transaction-denied! event-publisher tx event-id)
                    )
                )
                "denied"
            )
        )
    )
)

(defn get-transaction-by-id [repository id]
    (ports/get-by-id repository :transactions id)
)

(defn get-all-transactions [repository]
    (ports/get-all repository :transactions)
)

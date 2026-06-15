(ns bank.application.loan-service
    (:require [bank.domain.loan :as domain]
              [bank.ports.repository :as ports]
              [bank.ports.event-publisher :as event-ports]
    )
)

(defn create-loan! [repository event-publisher principal rate inception-date term investor issuer]
    ;; Creates the loan and enqueues the Transaction.requested settlement event in one transaction, so the loan can't be
    ;; persisted without its settlement request (or vice versa). The relay publishes the queued event to Kafka. investor and
    ;; issuer are the resolved records, whose :id becomes the loan's party and whose :account-id becomes the transfer's
    ;; source/destination.
    (ports/with-tx repository
        (fn [tx]
            (let [loan-id (java.util.UUID/randomUUID)
                  loan (domain/create-loan loan-id principal rate inception-date term (:id investor) (:id issuer) "created")]
                (ports/insert! tx :loans loan)
                (event-ports/enqueue-transaction-requested! event-publisher tx loan-id (:principal loan) (:account-id investor) (:account-id issuer))
                loan
            )
        )
    )
)

(defn update-loan-status [repository id new-status]
    (if-let [current-loan (ports/get-by-id repository :loans id)]
        (let [updated-loan (domain/update-loan-status current-loan new-status)]
            (fn [] (ports/update! repository :loans id {:status (get updated-loan :status)}))
        )
        (throw (ex-info "Loan not found!" {:id id}))
    )
)

(defn get-loan-by-id [repository id]
    (ports/get-by-id repository :loans id)
)

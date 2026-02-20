(ns bank.domain.events
    (:require [bank.domain.util :as util])
)

(defrecord TransactionRequestEvent [id value source-account-id destination-account-id])
(defrecord TransactionApprovalEvent [id])
(defrecord TransactionDenialEvent [id])

(defn create-transaction-request-event [id value source-account-id destination-account-id]
    (let [event (->TransactionRequestEvent id value source-account-id destination-account-id)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the transaction request event, invalid ID!" {:event event}))

            (not (and (util/bigdec? value) (> value 0.00M)))
            (throw (ex-info "Failed to create the transaction request event, invalid transaction value!" {:event event}))

            (not (uuid? source-account-id))
            (throw (ex-info "Failed to create the transaction request event, invalid source account ID!" {:event event}))

            (not (uuid? destination-account-id))
            (throw (ex-info "Failed to create the transaction request event, invalid destination account ID!" {:event event}))
        )
        event
    )
)

(defn create-transaction-approval-event [id]
    (let [event (->TransactionApprovalEvent id)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the transaction approval event, invalid ID!" {:event event}))
        )
        event
    )
)

(defn create-transaction-denial-event [id]
    (let [event (->TransactionDenialEvent id)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the transaction denial event, invalid ID!" {:event event}))
        )
        event
    )
)

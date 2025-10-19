(ns bank.domain.transaction
    (:require [bank.domain.util :as util])
)

(def transaction-types #{"deposit" "withdrawal" "transfer"})

(defrecord Transaction [id type value source-account-id destination-account-id])

(defn create-transaction [id type value source-account-id destination-account-id]
    (let [transaction (->Transaction id type value source-account-id destination-account-id)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the transaction, invalid ID!" {:transaction transaction}))

            (not (contains? transaction-types type))
            (throw (ex-info "Failed to create the transaction, invalid transaction type!" {:transaction transaction}))

            (not (and (util/bigdec? value) (> value 0.00M)))
            (throw (ex-info "Failed to create the transaction, invalid transaction value!" {:transaction transaction}))

            (and (or (= type "withdrawal") (= type "transfer")) (not (uuid? source-account-id)))
            (throw (ex-info "Failed to create the transaction, invalid source account ID!" {:transaction transaction}))

            (and (= type "deposit") (not (nil? source-account-id)))
            (throw (ex-info "Failed to create the transaction, invalid source account ID!" {:transaction transaction}))

            (and (or (= type "deposit") (= type "transfer")) (not (uuid? destination-account-id)))
            (throw (ex-info "Failed to create the transaction, invalid destination account ID!" {:transaction transaction}))

            (and (= type "withdrawal") (not (nil? destination-account-id)))
            (throw (ex-info "Failed to create the transaction, invalid destination account ID!" {:transaction transaction}))
        )
        transaction
    )
)
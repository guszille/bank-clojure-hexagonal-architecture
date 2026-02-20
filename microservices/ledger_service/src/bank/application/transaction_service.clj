(ns bank.application.transaction-service
    (:require [bank.domain.transaction :as domain]
              [bank.ports.repository :as ports]
    )
)

(defn create-transaction [repository type value source-account-id destination-account-id]
    (let [transaction-id (java.util.UUID/randomUUID)
          transaction (domain/create-transaction transaction-id type value source-account-id destination-account-id)]
        (ports/insert! repository :transactions transaction)
        transaction
    )
)

(defn get-transaction-by-id [repository id]
    (ports/get-by-id repository :transactions id)
)

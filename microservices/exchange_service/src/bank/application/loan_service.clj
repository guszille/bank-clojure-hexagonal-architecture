(ns bank.application.loan-service
    (:require [bank.domain.loan :as domain]
              [bank.ports.repository :as ports]
    )
)

(defn create-loan [repository principal rate inception-date term investor-id issuer-id]
    (let [loan-id (java.util.UUID/randomUUID)
          loan (domain/create-loan loan-id principal rate inception-date term investor-id issuer-id)]
        (ports/insert! repository :loans loan)
        loan
    )
)

(defn get-loan-by-id [repository id]
    (ports/get-by-id repository :loans id)
)
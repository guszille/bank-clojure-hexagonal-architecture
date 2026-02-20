(ns bank.application.loan-service
    (:require [bank.domain.loan :as domain]
              [bank.ports.repository :as ports]
    )
)

(defn create-loan [repository principal rate inception-date term investor-id issuer-id]
    (let [loan-id (java.util.UUID/randomUUID)
          loan (domain/create-loan loan-id principal rate inception-date term investor-id issuer-id "created")]
        (ports/insert! repository :loans loan)
        loan
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

(ns bank.application.issuer-service
    (:require [bank.domain.issuer :as domain]
              [bank.ports.repository :as ports]
    )
)

(defn create-issuer [repository account-id]
    (let [issuer-id (java.util.UUID/randomUUID)
          issuer (domain/create-issuer issuer-id account-id)]
        (ports/insert! repository :issuers issuer)
        issuer
    )
)

(defn get-issuer-by-id [repository id]
    (ports/get-by-id repository :issuers id)
)

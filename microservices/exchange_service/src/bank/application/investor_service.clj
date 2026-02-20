(ns bank.application.investor-service
    (:require [bank.domain.investor :as domain]
              [bank.ports.repository :as ports]
    )
)

(defn create-investor [repository account-id]
    (let [investor-id (java.util.UUID/randomUUID)
          investor (domain/create-investor investor-id account-id)]
        (ports/insert! repository :investors investor)
        investor
    )
)

(defn get-investor-by-id [repository id]
    (ports/get-by-id repository :investors id)
)

(ns bank.application.account-service
    (:require [bank.domain.account :as domain]
              [bank.ports.repository :as ports]
    )
)

(defn create-account [repository]
    (let [account-id (java.util.UUID/randomUUID)
          account (domain/create-account account-id (ports/get-next-account-number repository) (bigdec 0.00))]
        (ports/insert! repository :accounts account)
        account
    )
)

(defn update-account-balance [repository id value]
    (if-let [current-account (ports/get-by-id repository :accounts id)]
        (let [updated-account (domain/update-account-balance current-account value)]
            (fn [] (ports/update! repository :accounts id {:balance (get updated-account :balance)}))
        )
        (throw (ex-info "Account not found!" {:id id}))
    )
)

(defn get-account-by-id [repository id]
    (ports/get-by-id repository :accounts id)
)

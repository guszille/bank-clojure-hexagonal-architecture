(ns bank.application.account-service
    (:require [bank.domain.account :as domain]
              [bank.ports.repository :as ports]
    )
)

(def next-available-account-number (atom 1))

(defn get-naan []
    (let [number (format "%05d" @next-available-account-number)]
        (swap! next-available-account-number inc)
        number
    )
)

(defn create-account [repository]
    (let [account-id (java.util.UUID/randomUUID)
          account-number (get-naan)
          account-balance (bigdec 0.00)
          account (domain/create-account account-id account-number account-balance)]
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

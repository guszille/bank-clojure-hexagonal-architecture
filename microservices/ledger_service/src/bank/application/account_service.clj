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

(defn apply-balance-delta! [tx id delta]
    ;; Locks the account row, applies the delta through the domain (which enforces balance >= 0), and persists the new balance.
    ;; Must run inside a (ports/with-tx ...) body.
    (if-let [current-account (ports/get-for-update tx :accounts id)]
        (let [updated-account (domain/update-account-balance current-account delta)]
            (ports/update! tx :accounts id {:balance (get updated-account :balance)})
        )
        (throw (ex-info "Account not found!" {:id id}))
    )
)

(defn get-account-by-id [repository id]
    (ports/get-by-id repository :accounts id)
)

(defn get-all-accounts [repository]
    (ports/get-all repository :accounts)
)

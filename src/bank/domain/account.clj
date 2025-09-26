(ns bank.domain.account)

(defrecord Account [id number balance])

(defn create-account [id number balance]
    (let [account (->Account id number balance)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the account, invalid ID!" {:account account}))

            (not (and (string? number) (= (count number) 5)))
            (throw (ex-info "Failed to create the account, invalid account number!" {:account account}))

            (not (and (number? balance) (>= balance 0)))
            (throw (ex-info "Failed to create the account, invalid account balance!" {:account account}))
        )
        account
    )
)

(defn update-account-balance [account value]
    {:pre [(instance? Account account) (number? value)]}
    (let [{id :id number :number balance :balance} account]
        (let [updated-balance (+ balance value)
              updated-account (->Account id number updated-balance)]
            (when (< updated-balance 0) (throw (ex-info "Failed to update the account, balance must be greater than or equal to zero!" {:account updated-account})))
            updated-account
        )
    )
)
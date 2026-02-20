(ns bank.domain.issuer
    (:require [bank.domain.util :as util])
)

(defrecord Issuer [id account-id])

(defn create-issuer [id account-id]
    (let [issuer (->Issuer id account-id)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the issuer, invalid ID!" {:issuer issuer}))

            (not (uuid? account-id))
            (throw (ex-info "Failed to create the issuer, invalid account ID!" {:issuer issuer}))
        )
        issuer
    )
)

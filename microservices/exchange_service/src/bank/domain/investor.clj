(ns bank.domain.investor
    (:require [bank.domain.util :as util])
)

(defrecord Investor [id account-id])

(defn create-investor [id account-id]
    (let [investor (->Investor id account-id)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the investor, invalid ID!" {:investor investor}))

            (not (uuid? account-id))
            (throw (ex-info "Failed to create the investor, invalid account ID!" {:investor investor}))
        )
        investor
    )
)
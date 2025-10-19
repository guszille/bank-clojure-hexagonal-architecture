(ns bank.domain.loan
    (:require [bank.domain.util :as util])
)

(defrecord Loan [id principal rate inception-date term investor-id issuer-id])

(defn create-loan [id principal rate inception-date term investor-id issuer-id]
    (let [loan (->Loan id principal rate inception-date term investor-id issuer-id)]
        (cond
            (not (uuid? id))
            (throw (ex-info "Failed to create the loan, invalid ID!" {:loan loan}))

            (not (and (util/bigdec? principal) (>= principal 0.00M)))
            (throw (ex-info "Failed to create the loan, invalid principal!" {:loan loan}))

            (not (and (util/bigdec? rate) (>= rate 0.00M)))
            (throw (ex-info "Failed to create the loan, invalid rate!" {:loan loan}))

            (not (util/isodate? inception-date))
            (throw (ex-info "Failed to create the loan, invalid inception date!" {:loan loan}))

            (not (and (integer? term) (> term 0)))
            (throw (ex-info "Failed to create the loan, invalid term!" {:loan loan}))

            (not (uuid? investor-id))
            (throw (ex-info "Failed to create the loan, invalid investor ID!" {:loan loan}))

            (not (uuid? issuer-id))
            (throw (ex-info "Failed to create the loan, invalid issuer ID!" {:loan loan}))
        )
        loan
    )
)
(ns bank.domain.loan
    (:require [bank.domain.util :as util])
)

(def loan-statuses #{"created" "approved" "denied"})

(defrecord Loan [id principal rate inception-date term investor-id issuer-id status])

(defn create-loan [id principal rate inception-date term investor-id issuer-id status]
    (let [loan (->Loan id principal rate inception-date term investor-id issuer-id status)]
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

            (not (contains? loan-statuses status))
            (throw (ex-info "Failed to create the loan, invalid loan status!" {:loan loan}))
        )
        loan
    )
)

(defn update-loan-status [loan new-status]
    {:pre [(instance? Loan loan) (string? new-status)]}
    (let [{id :id principal :principal rate :rate inception-date :inception-date term :term investor-id :investor-id issuer-id :issuer-id status :status} loan]
        (let [updated-loan (->Loan id principal rate inception-date term investor-id issuer-id new-status)]
            (when (not (= status "created")) (throw (ex-info "Failed to update the loan, loan already approved or denied!" {:loan loan})))
            updated-loan
        )
    )
)

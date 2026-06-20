(ns bank-ui.views.issuers
    (:require [bank-ui.views.party :as party]
              [bank-ui.subs :as subs]
              [bank-ui.events :as events]
    )
)

(defn page []
    [party/page {:title "Issuers"
                 :singular "issuer"
                 :sub ::subs/issuers
                 :fetch ::events/fetch-issuers
                 :create ::events/create-issuer}]
)

(ns bank-ui.views.investors
    (:require [bank-ui.views.party :as party]
              [bank-ui.subs :as subs]
              [bank-ui.events :as events]
    )
)

(defn page []
    [party/page {:title "Investors"
                 :singular "investor"
                 :sub ::subs/investors
                 :fetch ::events/fetch-investors
                 :create ::events/create-investor}]
)

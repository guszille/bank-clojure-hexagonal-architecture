(ns bank-ui.views.transactions
    (:require [re-frame.core :as rf]
              [reagent.core :as r]
              [bank-ui.subs :as subs]
              [bank-ui.events :as events]
              [bank-ui.util.money :as money]
              [bank-ui.views.common :as common]
    )
)

(defn- account-options [accounts]
    (for [a accounts]
        ^{:key (:id a)}
        [:option {:value (:id a)} (str (:number a) " — " (money/display (:balance a)))]
    )
)

(defn- tx-table [txs]
    (if (empty? txs)
        [:p.empty "No transactions yet."]
        [:table
         [:thead [:tr [:th "Type"] [:th "Value"] [:th "Source"] [:th "Destination"] [:th "ID"]]]
         [:tbody
          (for [t txs]
              ^{:key (:id t)}
              [:tr
               [:td [:span.badge (:type t)]]
               [:td.money (money/display (:value t))]
               [:td.mono (when-let [s (:source-account-id t)] [:span {:title (str s)} (common/short-id s)])]
               [:td.mono (when-let [d (:destination-account-id t)] [:span {:title (str d)} (common/short-id d)])]
               [:td.mono {:title (str (:id t))} (common/short-id (:id t))]]
          )]]
    )
)

(defn page []
    (r/with-let [form (r/atom {:type "deposit"})
                 _ (do (rf/dispatch [::events/fetch-transactions])
                     (rf/dispatch [::events/fetch-accounts])
                 )]
        (let [txs @(rf/subscribe [::subs/transactions])
              accounts @(rf/subscribe [::subs/accounts])
              t (:type @form)]
            [:section
             [:h1 "Transactions"]
             [:form.card
              {:on-submit (fn [e]
                  (.preventDefault e)
                  (rf/dispatch [::events/create-transaction @form])
                  (reset! form {:type t})
              )}
              [:label "Type"
               [:select {:value t :on-change #(swap! form assoc :type (.. % -target -value))}
                [:option {:value "deposit"} "Deposit"]
                [:option {:value "withdrawal"} "Withdrawal"]
                [:option {:value "transfer"} "Transfer"]]]
              [:label "Value"
               [:input {:type "text" :placeholder "100.00" :value (:value @form "")
                        :on-change #(swap! form assoc :value (.. % -target -value))}]]
              (when (#{"withdrawal" "transfer"} t)
                  [:label "Source account"
                   [:select {:value (:source-account-id @form "")
                             :on-change #(swap! form assoc :source-account-id (.. % -target -value))}
                    [:option {:value ""} "— select —"]
                    (account-options accounts)]]
              )
              (when (#{"deposit" "transfer"} t)
                  [:label "Destination account"
                   [:select {:value (:destination-account-id @form "")
                             :on-change #(swap! form assoc :destination-account-id (.. % -target -value))}
                    [:option {:value ""} "— select —"]
                    (account-options accounts)]]
              )
              [:button.primary {:type "submit"} "Submit transaction"]]
             (tx-table txs)]
        )
    )
)

(ns bank-ui.views.accounts
    (:require [re-frame.core :as rf]
              [reagent.core :as r]
              [bank-ui.subs :as subs]
              [bank-ui.events :as events]
              [bank-ui.util.money :as money]
              [bank-ui.views.common :as common]
    )
)

(defn page []
    (r/with-let [_ (rf/dispatch [::events/fetch-accounts])]
        (let [accounts @(rf/subscribe [::subs/accounts])]
            [:section
             [:div.section-head
              [:h1 "Accounts"]
              [:button.primary {:on-click #(rf/dispatch [::events/create-account])} "+ New account"]]
             (if (empty? accounts)
                 [:p.empty "No accounts yet. Create one to get started."]
                 [:table
                  [:thead [:tr [:th "Number"] [:th "Balance"] [:th "ID"]]]
                  [:tbody
                   (for [a accounts]
                       ^{:key (:id a)}
                       [:tr
                        [:td (:number a)]
                        [:td.money (money/display (:balance a))]
                        [:td.mono {:title (str (:id a))} (common/short-id (:id a))]]
                   )]]
             )]
        )
    )
)

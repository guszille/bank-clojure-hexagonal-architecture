(ns bank-ui.views.layout
    (:require [re-frame.core :as rf]
              [bank-ui.subs :as subs]
              [bank-ui.events :as events]
              [bank-ui.views.accounts :as accounts]
              [bank-ui.views.transactions :as transactions]
              [bank-ui.views.investors :as investors]
              [bank-ui.views.issuers :as issuers]
              [bank-ui.views.loans :as loans]
    )
)

(def nav-items
    [[:accounts "Accounts" "/"]
     [:transactions "Transactions" "/transactions"]
     [:investors "Investors" "/investors"]
     [:issuers "Issuers" "/issuers"]
     [:loans "Loans" "/loans"]]
)

(defn navbar [route]
    [:nav.nav
     [:span.brand "🏦 Bank Console"]
     [:ul
      (for [[id label path] nav-items]
          ^{:key id}
          [:li [:a {:href path :class (when (= id route) "active")} label]]
      )]]
)

(defn error-banner [error]
    (when error
        [:div.error-banner
         [:span error]
         [:button {:on-click #(rf/dispatch [::events/clear-error])} "×"]]
    )
)

(defn app []
    (let [route @(rf/subscribe [::subs/route])
          error @(rf/subscribe [::subs/error])]
        [:div.app
         [navbar route]
         [error-banner error]
         [:main.container
          (case route
              :accounts [accounts/page]
              :transactions [transactions/page]
              :investors [investors/page]
              :issuers [issuers/page]
              :loans [loans/page]
              [accounts/page]
          )]]
    )
)

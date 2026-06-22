(ns bank-ui.views.loans
    (:require [re-frame.core :as rf]
              [reagent.core :as r]
              [bank-ui.subs :as subs]
              [bank-ui.events :as events]
              [bank-ui.util.money :as money]
              [bank-ui.views.common :as common]
    )
)

(defn- status-badge [status]
    [:span.badge {:class (case status
        "approved" "ok"
        "denied" "bad"
        "pending"
    )}
        status
    ]
)

(defn- loans-table [loans]
    (if (empty? loans)
        [:p.empty "No loans yet."]
        [:table
            [:thead [:tr [:th "Status"] [:th "Principal"] [:th "Rate"] [:th "Term"] [:th "Inception"] [:th "ID"]]]
            [:tbody
                (for [l loans]
                    ^{:key (:id l)}
                    [:tr
                        [:td (status-badge (:status l))]
                        [:td.money (money/display (:principal l))]
                        [:td.money (money/display (:rate l))]
                        [:td (:term l)]
                        [:td (:inception-date l)]
                        [:td.mono {:title (str (:id l))} (common/short-id (:id l))]
                    ]
                )
            ]
        ]
    )
)

(defn page []
    (r/with-let [form (r/atom {:term "6"})
                 _ (do (rf/dispatch [::events/fetch-loans])
                     (rf/dispatch [::events/fetch-investors])
                     (rf/dispatch [::events/fetch-issuers])
                 )]
        (let [loans @(rf/subscribe [::subs/loans])
              investors @(rf/subscribe [::subs/investors])
              issuers @(rf/subscribe [::subs/issuers])
              on-val (fn [k] #(swap! form assoc k (.. % -target -value)))]
            [:section
                [:h1 "Loans"]
                [:p.hint "Loans settle asynchronously over Kafka — a new loan starts as "
                    [:span.badge.pending "created"] " and updates live to "
                    [:span.badge.ok "approved"] " or " [:span.badge.bad "denied"] "."
                ]
                [:form.card.loan-form
                    {:on-submit (fn [e]
                        (.preventDefault e)
                        (rf/dispatch [::events/create-loan @form])
                        (reset! form {:term (:term @form)})
                    )}
                    [:label "Principal" [:input {:type "text" :placeholder "1000.00"
                                                 :value (:principal @form "") :on-change (on-val :principal)}]]
                    [:label "Rate" [:input {:type "text" :placeholder "10.00"
                                            :value (:rate @form "") :on-change (on-val :rate)}]]
                    [:label "Inception date" [:input {:type "date"
                                                      :value (:inception-date @form "") :on-change (on-val :inception-date)}]]
                    [:label "Term (months)" [:input {:type "number" :min 1
                                                     :value (:term @form "") :on-change (on-val :term)}]]
                    [:label "Investor"
                        [:select {:value (:investor-id @form "") :on-change (on-val :investor-id)}
                            [:option {:value ""} "— select —"]
                            (for [i investors] ^{:key (:id i)} [:option {:value (:id i)} (common/short-id (:id i))])
                        ]
                    ]
                    [:label "Issuer"
                        [:select {:value (:issuer-id @form "") :on-change (on-val :issuer-id)}
                            [:option {:value ""} "— select —"]
                            (for [i issuers] ^{:key (:id i)} [:option {:value (:id i)} (common/short-id (:id i))])
                        ]
                    ]
                    [:button.primary {:type "submit"} "Create loan"]
                ]
                (loans-table loans)
            ]
        )
    )
)

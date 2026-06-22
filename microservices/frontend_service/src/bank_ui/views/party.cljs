(ns bank-ui.views.party
    "Shared UI for investors and issuers -- both entities just link a ledger account."
    (:require [re-frame.core :as rf]
              [reagent.core :as r]
              [clojure.string :as str]
              [bank-ui.subs :as subs]
              [bank-ui.events :as events]
              [bank-ui.views.common :as common]
    )
)

(defn page
    "opts: {:title \"Investors\" :singular \"investor\"
          :sub ::subs/investors :fetch ::events/fetch-investors :create ::events/create-investor}"
    [{:keys [title singular sub fetch create]}]
    (r/with-let [form (r/atom {})
                 _ (do (rf/dispatch [fetch])
                     (rf/dispatch [::events/fetch-accounts])
                 )]
        (let [items @(rf/subscribe [sub])
              accounts @(rf/subscribe [::subs/accounts])]
            [:section
                [:h1 title]
                [:form.card
                    {:on-submit (fn [e]
                        (.preventDefault e)
                        (when-let [aid (not-empty (:account-id @form))]
                            (rf/dispatch [create aid])
                        )
                        (reset! form {})
                    )}
                    [:label "Ledger account"
                        [:select {:value (:account-id @form "")
                                  :on-change #(swap! form assoc :account-id (.. % -target -value))}
                            [:option {:value ""} "— select —"]
                            (for [a accounts] ^{:key (:id a)} [:option {:value (:id a)} (:number a)])
                        ]
                    ]
                    [:button.primary {:type "submit"} (str "Register " singular)]
                ]
                (if (empty? items)
                    [:p.empty (str "No " (str/lower-case title) " yet.")]
                    [:table
                        [:thead [:tr [:th "ID"] [:th "Account ID"]]]
                        [:tbody
                            (for [i items]
                                ^{:key (:id i)}
                                [:tr
                                    [:td.mono {:title (str (:id i))} (common/short-id (:id i))]
                                    [:td.mono {:title (str (:account-id i))} (common/short-id (:account-id i))]
                                ]
                            )
                        ]
                    ]
                )
            ]
        )
    )
)

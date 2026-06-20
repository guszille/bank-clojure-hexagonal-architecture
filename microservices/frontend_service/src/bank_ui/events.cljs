(ns bank-ui.events
    (:require [re-frame.core :as rf]
              [ajax.core :as ajax]
              [day8.re-frame.http-fx] ;; registers the :http-xhrio effect
              [clojure.string :as str]
              [bank-ui.db :as db]
              [bank-ui.config :as config]
              [bank-ui.util.money :as money]
    )
)

;; ---------------------------------------------------------------------------
;; Bootstrap / routing / errors
;; ---------------------------------------------------------------------------
(rf/reg-event-db ::initialize-db (fn [_ _] db/default-db))
(rf/reg-event-db ::set-route (fn [db [_ route]] (assoc db :route (or route :accounts))))
(rf/reg-event-db ::clear-error (fn [db _] (assoc db :error nil)))

(rf/reg-event-db
    ::request-failed
    (fn [db [_ resp]]
        (assoc db :error (or (get-in resp [:response :error])
            (:status-text resp)
            "Request failed"
        ))
    )
)

;; ---------------------------------------------------------------------------
;; Request helpers
;; ---------------------------------------------------------------------------
(defn- json-get [uri on-success]
    {:method :get
     :uri uri
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success on-success
     :on-failure [::request-failed]}
)

(defn- json-post [uri body on-success]
    {:method :post
     :uri uri
     :params body
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success on-success
     :on-failure [::request-failed]}
)

(defn- blank->nil [s] (when-not (str/blank? (str s)) s))
(defn- compact [m] (into {} (remove (comp nil? val) m)))

;; ---------------------------------------------------------------------------
;; Accounts (ledger)
;; ---------------------------------------------------------------------------
(rf/reg-event-fx ::fetch-accounts
    (fn [_ _] {:http-xhrio (json-get (str config/ledger-base "/accounts") [::set-accounts])})
)

(rf/reg-event-db ::set-accounts
    (fn [db [_ accounts]] (assoc db :accounts (vec accounts)))
)

(rf/reg-event-fx ::create-account
    (fn [_ _]
        {:http-xhrio {:method :post
                      :uri (str config/ledger-base "/accounts")
                      :format (ajax/json-request-format)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [::fetch-accounts]
                      :on-failure [::request-failed]}}
    )
)

;; ---------------------------------------------------------------------------
;; Transactions (ledger)
;; ---------------------------------------------------------------------------
(rf/reg-event-fx ::fetch-transactions
    (fn [_ _] {:http-xhrio (json-get (str config/ledger-base "/transactions") [::set-transactions])})
)

(rf/reg-event-db ::set-transactions
    (fn [db [_ txs]] (assoc db :transactions (vec txs)))
)

(rf/reg-event-fx ::create-transaction
    (fn [_ [_ form]]
        (let [body (compact {:type (:type form)
                             :value (money/->wire (:value form))
                             :source-account-id (blank->nil (:source-account-id form))
                             :destination-account-id (blank->nil (:destination-account-id form))})]
            {:http-xhrio (json-post (str config/ledger-base "/transactions") body [::transaction-created])}
        )
    )
)

;; A money movement changes balances, so refresh both lists.
(rf/reg-event-fx ::transaction-created
    (fn [_ _] {:fx [[:dispatch [::fetch-transactions]]
                    [:dispatch [::fetch-accounts]]]})
)

;; ---------------------------------------------------------------------------
;; Investors (exchange)
;; ---------------------------------------------------------------------------
(rf/reg-event-fx ::fetch-investors
    (fn [_ _] {:http-xhrio (json-get (str config/exchange-base "/investors") [::set-investors])})
)

(rf/reg-event-db ::set-investors
    (fn [db [_ xs]] (assoc db :investors (vec xs)))
)

(rf/reg-event-fx ::create-investor
    (fn [_ [_ account-id]]
        {:http-xhrio (json-post (str config/exchange-base "/investors")
            {:account-id account-id} [::fetch-investors]
        )}
    )
)

;; ---------------------------------------------------------------------------
;; Issuers (exchange)
;; ---------------------------------------------------------------------------
(rf/reg-event-fx ::fetch-issuers
    (fn [_ _] {:http-xhrio (json-get (str config/exchange-base "/issuers") [::set-issuers])})
)

(rf/reg-event-db ::set-issuers
    (fn [db [_ xs]] (assoc db :issuers (vec xs)))
)

(rf/reg-event-fx ::create-issuer
    (fn [_ [_ account-id]]
        {:http-xhrio (json-post (str config/exchange-base "/issuers")
            {:account-id account-id} [::fetch-issuers]
        )}
    )
)

;; ---------------------------------------------------------------------------
;; Loans (exchange) -- created async, then settled over Kafka. We poll until settled.
;; ---------------------------------------------------------------------------
(rf/reg-event-fx ::fetch-loans
    (fn [_ _] {:http-xhrio (json-get (str config/exchange-base "/loans") [::set-loans])})
)

(rf/reg-event-db ::set-loans
    (fn [db [_ xs]] (assoc db :loans (vec xs)))
)

(rf/reg-event-fx ::create-loan
    (fn [_ [_ form]]
        (let [body {:principal (money/->wire (:principal form))
                    :rate (money/->wire (:rate form))
                    :inception-date (:inception-date form)
                    :term (js/parseInt (:term form) 10)
                    :investor-id (:investor-id form)
                    :issuer-id (:issuer-id form)}]
            {:http-xhrio (json-post (str config/exchange-base "/loans") body [::loan-created])}
        )
    )
)

(rf/reg-event-fx ::loan-created
    (fn [_ [_ loan]]
        {:fx [[:dispatch [::fetch-loans]]
              [:dispatch-later {:ms config/loan-poll-ms :dispatch [::poll-loan (:id loan)]}]]}
    )
)

(rf/reg-event-fx ::poll-loan
    (fn [_ [_ id]]
        {:http-xhrio (json-get (str config/exchange-base "/loans/" id) [::loan-polled id])}
    )
)

;; Merge the polled loan into the list; keep polling only while it is still "created".
(rf/reg-event-fx ::loan-polled
    (fn [{:keys [db]} [_ id loan]]
        (let [present? (some #(= (:id %) (:id loan)) (:loans db))
              loans (if present?
                  (mapv #(if (= (:id %) (:id loan)) loan %) (:loans db))
                  (conj (:loans db) loan)
              )
              settled? (not= "created" (:status loan))]
            (cond-> {:db (assoc db :loans loans)}
                (not settled?)
                (assoc :fx [[:dispatch-later {:ms config/loan-poll-ms :dispatch [::poll-loan id]}]])
            )
        )
    )
)

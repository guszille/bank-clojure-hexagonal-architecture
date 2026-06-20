(ns bank-ui.routes
    (:require [reitit.frontend :as reitit]
              [reitit.frontend.easy :as rfe]
              [re-frame.core :as rf]
              [bank-ui.events :as events]
    )
)

(def routes
    [["/" {:name :accounts}]
     ["/transactions" {:name :transactions}]
     ["/investors" {:name :investors}]
     ["/issuers" {:name :issuers}]
     ["/loans" {:name :loans}]]
)

(defn- on-navigate [match & _]
    (rf/dispatch [::events/set-route (some-> match :data :name)])
)

(defn init! []
    ;; HTML5 history (no #fragment); the SPA nginx falls back to index.html so deep links work.
    (rfe/start! (reitit/router routes) on-navigate {:use-fragment false})
)

(def href rfe/href)

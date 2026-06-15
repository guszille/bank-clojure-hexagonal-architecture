(ns bank.application.loan-service-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.application.loan-service :as loan-service]
              [bank.adapters.outbox-publisher :as outbox-publisher]
              [bank.domain.investor :as investor]
              [bank.domain.issuer :as issuer]
              [bank.ports.repository :as ports]
    )
    (:import (java.util UUID))
)

;; An in-memory Repository modelling the unit-of-work contract: with-tx snapshots state, runs the
;; body, and restores on a throw. The real OutboxEventPublisher is used against it, since it only
;; writes outbox rows through the repository port.
(defn- fake-repo []
    (let [loans (atom {})
          outbox (atom {})
          tbl (fn [t] (case t :loans loans :outbox outbox))]
        {:loans loans
         :outbox outbox
         :repo (reify ports/Repository
                   (with-tx [this f]
                       (let [loans-snapshot @loans
                             outbox-snapshot @outbox]
                           (try
                               (f this)
                               (catch Throwable e
                                   (reset! loans loans-snapshot)
                                   (reset! outbox outbox-snapshot)
                                   (throw e)
                               )
                           )
                       )
                   )
                   (insert! [this table item]
                       (swap! (tbl table) assoc (:id item) item)
                       item
                   )
                   (update! [this table id args]
                       (swap! (tbl table) update id merge args)
                       (get @(tbl table) id)
                   )
                   (delete! [this table id]
                       (swap! (tbl table) dissoc id)
                   )
                   (get-by-id [this table id]
                       (get @(tbl table) id)
                   )
                   (get-unsent-outbox-events [this] (vec (vals @outbox)))
                   (mark-outbox-sent! [this id] (swap! outbox dissoc id))
               )
        }
    )
)

(deftest create-loan-persists-and-enqueues-atomically
    (let [investor-account-id (UUID/randomUUID)
          issuer-account-id (UUID/randomUUID)
          inv (investor/->Investor (UUID/randomUUID) investor-account-id)
          iss (issuer/->Issuer (UUID/randomUUID) issuer-account-id)
          {:keys [repo loans outbox]} (fake-repo)
          publisher (outbox-publisher/create-publisher)
          loan (loan-service/create-loan! repo publisher 100.00M 10.00M "2025-01-01" 6 inv iss)]
        (testing "the loan is created with status \"created\""
            (is (= "created" (:status loan)))
            (is (= 1 (count @loans)))
        )
        (testing "exactly one Transaction.requested event is queued, keyed by the loan id"
            (is (= 1 (count @outbox)))
            (let [row (first (vals @outbox))]
                (is (= "Transaction.requested" (:topic row)))
                (is (= (str (:id loan)) (:event-key row)))
            )
        )
    )
)

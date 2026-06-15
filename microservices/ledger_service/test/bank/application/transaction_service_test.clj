(ns bank.application.transaction-service-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.application.transaction-service :as transaction-service]
              [bank.adapters.outbox-publisher :as outbox-publisher]
              [bank.domain.account :as account]
              [bank.ports.repository :as ports]
    )
    (:import (java.util UUID))
)

;; An in-memory Repository that models the unit-of-work contract: with-tx snapshots the state, runs
;; the body, and restores the snapshot if the body throws. This lets us assert the application
;; orchestration is atomic, idempotent, and enqueues the right outbox events without standing up
;; Postgres. (Real SQL transaction / FOR UPDATE behavior belongs to an integration test with
;; Testcontainers.) The real OutboxEventPublisher is used against this fake, since it only writes
;; outbox rows through the repository port.
(defn- fake-repo
    ([initial-accounts] (fake-repo initial-accounts {}))
    ([initial-accounts {:keys [fail-transaction-insert?]}]
     (let [accounts (atom initial-accounts)
           transactions (atom {})
           processed-events (atom {})
           outbox (atom {})
           tbl (fn [t] (case t :accounts accounts :transactions transactions :processed-events processed-events :outbox outbox))]
         {:accounts accounts
          :transactions transactions
          :processed-events processed-events
          :outbox outbox
          :repo (reify ports/Repository
                    (with-tx [this f]
                        (let [accounts-snapshot @accounts
                              transactions-snapshot @transactions
                              processed-snapshot @processed-events
                              outbox-snapshot @outbox]
                            (try
                                (f this)
                                (catch Throwable e
                                    (reset! accounts accounts-snapshot)
                                    (reset! transactions transactions-snapshot)
                                    (reset! processed-events processed-snapshot)
                                    (reset! outbox outbox-snapshot)
                                    (throw e)
                                )
                            )
                        )
                    )
                    (insert! [this table item]
                        (when (and fail-transaction-insert? (= table :transactions))
                            (throw (ex-info "Simulated insert failure" {:table table}))
                        )
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
                    (get-for-update [this table id]
                        (get @(tbl table) id)
                    )
                    (get-next-account-number [this] "00001")
                    (get-unsent-outbox-events [this] (vec (vals @outbox)))
                    (mark-outbox-sent! [this id] (swap! outbox dissoc id))
                )
         }
    )
    )
)

(deftest transfer-moves-funds-atomically
    (let [src-id (UUID/randomUUID)
          dest-id (UUID/randomUUID)
          {:keys [repo accounts transactions]}
          (fake-repo {src-id (account/->Account src-id "00001" 100.00M)
                      dest-id (account/->Account dest-id "00002" 0.00M)})
          new-transaction (transaction-service/transfer! repo 30.00M src-id dest-id)]
        (testing "the transaction record is returned"
            (is (= "transfer" (:type new-transaction)))
            (is (= 30.00M (:value new-transaction)))
        )
        (testing "both balances are updated"
            (is (= 70.00M (:balance (get @accounts src-id))))
            (is (= 30.00M (:balance (get @accounts dest-id))))
        )
        (testing "exactly one transaction row is persisted"
            (is (= 1 (count @transactions)))
        )
    )
)

(deftest transfer-with-insufficient-funds-changes-nothing
    (let [src-id (UUID/randomUUID)
          dest-id (UUID/randomUUID)
          {:keys [repo accounts transactions]}
          (fake-repo {src-id (account/->Account src-id "00001" 50.00M)
                      dest-id (account/->Account dest-id "00002" 0.00M)})]
        (is (thrown? clojure.lang.ExceptionInfo
                (transaction-service/transfer! repo 100.00M src-id dest-id)))
        (testing "balances are unchanged"
            (is (= 50.00M (:balance (get @accounts src-id))))
            (is (= 0.00M (:balance (get @accounts dest-id))))
        )
        (testing "no transaction row is persisted"
            (is (empty? @transactions))
        )
    )
)

(deftest transfer-rolls-back-when-a-later-write-fails
    (let [src-id (UUID/randomUUID)
          dest-id (UUID/randomUUID)
          {:keys [repo accounts transactions]}
          (fake-repo {src-id (account/->Account src-id "00001" 100.00M)
                      dest-id (account/->Account dest-id "00002" 0.00M)}
                     {:fail-transaction-insert? true})]
        (is (thrown? clojure.lang.ExceptionInfo
                (transaction-service/transfer! repo 30.00M src-id dest-id)))
        (testing "both balance updates are rolled back when the transaction insert fails"
            (is (= 100.00M (:balance (get @accounts src-id))))
            (is (= 0.00M (:balance (get @accounts dest-id))))
        )
        (testing "no transaction row is persisted"
            (is (empty? @transactions))
        )
    )
)

(deftest transfer-rejects-self-transfer
    (let [id (UUID/randomUUID)
          {:keys [repo accounts transactions]}
          (fake-repo {id (account/->Account id "00001" 100.00M)})]
        (is (thrown? clojure.lang.ExceptionInfo
                (transaction-service/transfer! repo 10.00M id id)))
        (testing "the balance is untouched and no transaction is recorded"
            (is (= 100.00M (:balance (get @accounts id))))
            (is (empty? @transactions))
        )
    )
)

(deftest settlement-is-idempotent-and-enqueues-one-approved-event
    (let [event-id (UUID/randomUUID)
          src-id (UUID/randomUUID)
          dest-id (UUID/randomUUID)
          {:keys [repo accounts transactions processed-events outbox]}
          (fake-repo {src-id (account/->Account src-id "00001" 100.00M)
                      dest-id (account/->Account dest-id "00002" 0.00M)})
          publisher (outbox-publisher/create-publisher)]
        (testing "first delivery settles, records the outcome, and queues one approved event"
            (is (= "approved" (transaction-service/settle-requested-transfer! repo publisher event-id 30.00M src-id dest-id)))
            (is (= 70.00M (:balance (get @accounts src-id))))
            (is (= 30.00M (:balance (get @accounts dest-id))))
            (is (= 1 (count @transactions)))
            (is (= "approved" (:outcome (get @processed-events event-id))))
            (is (= 1 (count @outbox)))
            (let [row (first (vals @outbox))]
                (is (= "Transaction.approved" (:topic row)))
                (is (= (str event-id) (:event-key row)))
            )
        )
        (testing "redelivery replays the outcome without moving money or re-queuing"
            (is (= "approved" (transaction-service/settle-requested-transfer! repo publisher event-id 30.00M src-id dest-id)))
            (is (= 70.00M (:balance (get @accounts src-id))))
            (is (= 1 (count @transactions)))
            (is (= 1 (count @outbox)))
        )
    )
)

(deftest settlement-denial-is-recorded-replayed-and-enqueues-one-denied-event
    (let [event-id (UUID/randomUUID)
          src-id (UUID/randomUUID)
          dest-id (UUID/randomUUID)
          {:keys [repo accounts transactions processed-events outbox]}
          (fake-repo {src-id (account/->Account src-id "00001" 10.00M)
                      dest-id (account/->Account dest-id "00002" 0.00M)})
          publisher (outbox-publisher/create-publisher)]
        (testing "an underfunded settlement is denied, recorded, and queues one denied event"
            (is (= "denied" (transaction-service/settle-requested-transfer! repo publisher event-id 30.00M src-id dest-id)))
            (is (= 10.00M (:balance (get @accounts src-id))))
            (is (empty? @transactions))
            (is (= "denied" (:outcome (get @processed-events event-id))))
            (is (= 1 (count @outbox)))
            (is (= "Transaction.denied" (:topic (first (vals @outbox)))))
        )
        (testing "redelivery replays the denial without re-attempting or re-queuing"
            (is (= "denied" (transaction-service/settle-requested-transfer! repo publisher event-id 30.00M src-id dest-id)))
            (is (= 10.00M (:balance (get @accounts src-id))))
            (is (empty? @transactions))
            (is (= 1 (count @outbox)))
        )
    )
)

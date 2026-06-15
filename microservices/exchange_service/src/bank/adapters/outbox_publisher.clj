(ns bank.adapters.outbox-publisher
    (:require [cheshire.core :as json]
              [bank.ports.event-publisher :as ports]
              [bank.ports.repository :as repo]
              [bank.domain.events :as domain]
              [bank.adapters.util :as util]
    )
)

;; An EventPublisher that writes events into the transactional outbox instead of sending to Kafka directly. The write goes
;; through the caller's transaction connection (tx), so the event is persisted atomically with the business change. The relay
;; (bank.adapters.outbox-relay) does the actual Kafka send. The payload is composed with the trailing-M money encoding the
;; ledger expects.
(defrecord OutboxEventPublisher []
    ports/EventPublisher

    (enqueue-transaction-requested! [this tx event-id value source-account-id destination-account-id]
        (let [event (domain/create-transaction-request-event event-id value source-account-id destination-account-id)]
            (repo/insert! tx :outbox {:id (java.util.UUID/randomUUID)
                                      :topic "Transaction.requested"
                                      :event-key (str event-id)
                                      :payload (json/generate-string (util/compose-bigdec-fields event))})
        )
    )
)

(defn create-publisher []
    (->OutboxEventPublisher)
)

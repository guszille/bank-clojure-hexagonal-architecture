(ns bank.adapters.outbox-publisher
    (:require [cheshire.core :as json]
              [bank.ports.event-publisher :as ports]
              [bank.ports.repository :as repo]
              [bank.domain.events :as domain]
    )
)

;; An EventPublisher that writes events into the transactional outbox instead of sending to Kafka directly. The write goes
;; through the caller's transaction connection (tx), so the event is persisted atomically with the business change. The relay
;; (bank.adapters.outbox-relay) does the actual Kafka send.
(defrecord OutboxEventPublisher []
    ports/EventPublisher

    (enqueue-transaction-approved! [this tx event-id]
        (let [event (domain/create-transaction-approval-event event-id)]
            (repo/insert! tx :outbox {:id (java.util.UUID/randomUUID)
                                      :topic "Transaction.approved"
                                      :event-key (str event-id)
                                      :payload (json/generate-string event)})
        )
    )

    (enqueue-transaction-denied! [this tx event-id]
        (let [event (domain/create-transaction-denial-event event-id)]
            (repo/insert! tx :outbox {:id (java.util.UUID/randomUUID)
                                      :topic "Transaction.denied"
                                      :event-key (str event-id)
                                      :payload (json/generate-string event)})
        )
    )
)

(defn create-publisher []
    (->OutboxEventPublisher)
)

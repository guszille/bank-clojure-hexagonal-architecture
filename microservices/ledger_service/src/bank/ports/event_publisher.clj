(ns bank.ports.event-publisher)

;; An EventPublisher records an outbound event for publication. Implementations write to the transactional outbox on the
;; supplied transaction connection (tx), so the event is persisted atomically with the business change that produced it.
(defprotocol EventPublisher
    (enqueue-transaction-approved! [this tx event-id])
    (enqueue-transaction-denied! [this tx event-id])
)

(ns bank.ports.event-publisher)

(defprotocol EventPublisher
    (publish-transaction-approved! [this event-id])
    (publish-transaction-denied! [this event-id])
)

(ns bank.ports.event-publisher)

(defprotocol EventPublisher
    (publish-transaction-requested! [this event-id value source-account-id destination-account-id])
)

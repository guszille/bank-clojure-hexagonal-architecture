(ns bank.ports.repository)

(defprotocol Repository
    (with-tx [this f])
    (insert! [this table item])
    (update! [this table id args])
    (delete! [this table id])
    (get-by-id [this table id])
    (unsent-outbox-events [this])
    (mark-outbox-sent! [this id])
)

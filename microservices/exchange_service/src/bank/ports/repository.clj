(ns bank.ports.repository)

(defprotocol Repository
    (with-tx [this f])
    (insert! [this table item])
    (update! [this table id args])
    (delete! [this table id])
    (get-by-id [this table id])
    (get-all [this table])
    (get-unsent-outbox-events [this])
    (mark-outbox-sent! [this id])
)

(ns bank.ports.repository)

(defprotocol Repository
    (with-tx [this f])
    (insert! [this table item])
    (update! [this table id args])
    (delete! [this table id])
    (get-by-id [this table id])
    (get-all [this table])
    (get-for-update [this table id])
    (get-next-account-number [this])
    (get-unsent-outbox-events [this])
    (mark-outbox-sent! [this id])
)

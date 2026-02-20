(ns bank.ports.repository)

(defprotocol Repository
    (insert! [this table item])
    (update! [this table id args])
    (delete! [this table id])
    (get-by-id [this table id])
)

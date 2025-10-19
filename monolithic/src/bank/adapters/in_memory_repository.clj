(ns bank.adapters.in-memory-repository
    (:require [bank.ports.repository :as ports])
)

(defrecord InMemoryRepository [db]
    ports/Repository

    (insert! [_ table item]
        (swap! db assoc-in [table (:id item)] item)
    )
    (update! [_ table id args]
        (if-let [old-version (get-in (deref db) [table id])]
            (let [new-version (merge old-version args)]
                (swap! db assoc-in [table id] new-version)
            )
        )
    )
    (delete! [_ table id]
        (update-in db [table] dissoc id)
    )
    (get-by-id [_ table id]
        (get-in (deref db) [table id])
    )
)

(defn create-in-memory-repository []
    (->InMemoryRepository (atom {:accounts {} :transactions {}}))
)
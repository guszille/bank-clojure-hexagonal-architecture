(ns bank.adapters.in-memory-repository
    (:require [bank.ports.repository :as ports]))

(defrecord InMemoryRepository [db-atom]
    ports/AccountRepository
    (save [_ account]
        (swap! db-atom assoc (:id account) account))
    (get-by-id [_ id]
        (get @db-atom id)))

(defn create-in-memory-repository []
    (->InMemoryRepository (atom {})))
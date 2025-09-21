(ns bank.domain.account)

(defrecord Account [id number balance])

(defn create-account [id number]
    {:pre [(uuid? id) (and (string? number) (= (count number) 5))]}
    (->Account id number 0))
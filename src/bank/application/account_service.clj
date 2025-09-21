(ns bank.application.account-service
    (:require [bank.domain.account :as domain]
              [bank.ports.repository :as ports]))

(defn gen-account-number [] ;; FIXME: provisory solution for generating the account number.
    (apply str (repeatedly 5 #(rand-nth "0123456789"))))

(defn create-account [repository]
    (let [account-id (java.util.UUID/randomUUID)
          account-number (gen-account-number)
          account (domain/create-account account-id account-number)]
        (ports/save repository account)
        account))

(defn get-account-by-id [repository id]
    (ports/get-by-id repository id))
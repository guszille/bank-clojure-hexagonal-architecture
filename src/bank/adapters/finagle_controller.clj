(ns bank.adapters.finagle-controller
    (:require [cheshire.core :as json]
              [bank.application.account-service :as account-service]
              [bank.application.transaction-service :as transaction-service]
    )
    (:import (com.twitter.finagle Service Http)
             (com.twitter.finagle.http Request Response Status)
             (com.twitter.util Future)
    )
)

(defn- parse-json-request [request]
    (-> request .contentString (json/parse-string true))
)

(defn- to-json-response [status-code body]
    (let [response (Response/apply (Status/fromCode status-code))]
        (.setContentString response (json/generate-string body))
        (.put (.headerMap response) "content-type" "application/json; charset=utf-8")

        (Future/value response)
    )
)

(defn- parse-uuid-string [value]
    {:pre [(string? value)]}
    (if (not (clojure.string/blank? value)) (java.util.UUID/fromString value) nil)
)

(defn- handle-create-account [repository request]
    (try
        (let [new-account (account-service/create-account repository)]
            (to-json-response 201 new-account)
        )
        (catch Exception e
            (let [message (.getMessage e)]
                (to-json-response 400 {:error message})
            )
        )
    )
)

(defn- handle-get-account [repository request]
    (if-let [account-id (parse-uuid-string (last (re-find #"/accounts/(.*)" (.path request))))]
        (if-let [account (account-service/get-account-by-id repository account-id)]
            (to-json-response 200 account)
            (to-json-response 404 {:error "Account not found!"})
        )
        (to-json-response 400 {:error "Invalid account ID!"})
    )
)

(defn- handle-create-transaction [repository request]
    (try
        (let [request-body (parse-json-request request) type (get request-body :type nil) value (get request-body :value nil)]
            (if (and type value)
                (cond
                    (= type "withdrawal")
                    (if (contains? request-body :source-account-id)
                        (let [source-account-id (parse-uuid-string (get request-body :source-account-id))
                              update-account-handler (account-service/update-account-balance repository source-account-id (* -1 value))
                              new-transaction (transaction-service/create-transaction repository type value source-account-id nil)]
                            (update-account-handler) ;; Commit update.

                            (to-json-response 201 new-transaction)
                        )
                        (to-json-response 400 {:error "Malformed body!"})
                    )

                    (= type "deposit")
                    (if (contains? request-body :destination-account-id)
                        (let [destination-account-id (parse-uuid-string (get request-body :destination-account-id))
                              update-account-handler (account-service/update-account-balance repository destination-account-id value)
                              new-transaction (transaction-service/create-transaction repository type value nil destination-account-id)]
                            (update-account-handler) ;; Commit update.

                            (to-json-response 201 new-transaction)
                        )
                        (to-json-response 400 {:error "Malformed body!"})
                    )

                    (= type "transfer")
                    (if (and (contains? request-body :source-account-id) (contains? request-body :destination-account-id))
                        (let [source-account-id (parse-uuid-string (get request-body :source-account-id))
                              destination-account-id (parse-uuid-string (get request-body :destination-account-id))
                              update-account-handler-1 (account-service/update-account-balance repository source-account-id (* -1 value))
                              update-account-handler-2 (account-service/update-account-balance repository destination-account-id value)
                              new-transaction (transaction-service/create-transaction repository type value source-account-id destination-account-id)]
                            (update-account-handler-1) ;; Commit update 1.
                            (update-account-handler-2) ;; Commit update 2.

                            (to-json-response 201 new-transaction)
                        )
                        (to-json-response 400 {:error "Malformed body!"})
                    )
                )
                (to-json-response 400 {:error "Malformed body!"})
            )
        )
        (catch Exception e
            (let [message (.getMessage e)]
                (to-json-response 400 {:error message})
            )
        )
    )
)

(defn- handle-get-transaction [repository request]
    (if-let [transaction-id (parse-uuid-string (last (re-find #"/accounts/(.*)" (.path request))))]
        (if-let [transaction (transaction-service/get-transaction-by-id repository transaction-id)]
            (to-json-response 200 transaction 200)
            (to-json-response 404 {:error "Transaction not found!"})
        )
        (to-json-response 400 {:error "Invalid transaction ID!"})
    )
)

(defn create-server [port repository]
    (let [handler (proxy [Service] []
        (apply [request]
            (let [path (.path request) method (.method request)]
                (cond
                    (and (= (str method) "POST") (= path "/accounts"))
                    (handle-create-account repository request)

                    (and (= (str method) "GET") (re-matches #"/accounts/.*" path))
                    (handle-get-account repository request)

                    (and (= (str method) "POST") (= path "/transactions"))
                    (handle-create-transaction repository request)

                    (and (= (str method) "GET") (re-matches #"/transactions/.*" path))
                    (handle-get-transaction repository request)

                    :else
                    (to-json-response 404 {})
                )
            )
        ))]
        (Http/serve (str ":" port) handler)
    )
)
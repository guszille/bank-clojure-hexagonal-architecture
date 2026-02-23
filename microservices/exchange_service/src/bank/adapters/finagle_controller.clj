(ns bank.adapters.finagle-controller
    (:require [cheshire.core :as json]
              [bank.application.investor-service :as investor-service]
              [bank.application.issuer-service :as issuer-service]
              [bank.application.loan-service :as loan-service]
              [bank.ports.event-publisher :as ports]
              [bank.adapters.util :as util]
    )
    (:import (com.twitter.finagle Service Http)
             (com.twitter.finagle.http Request Response Status)
             (com.twitter.util Future)
    )
)

(defn- parse-json-request [request]
    (let [body (-> request .contentString (json/parse-string true))]
        (util/parse-bigdec-fields body)
    )
)

(defn- to-json-response [status-code body]
    (let [response (Response/apply (Status/fromCode status-code))]
        (.setContentString response (json/generate-string body))
        (.put (.headerMap response) "content-type" "application/json; charset=utf-8")

        (Future/value response)
    )
)

(defn- handle-create-investor [repository request]
    (try
        (let [request-body (parse-json-request request) account-id (get request-body :account-id nil)]
            (if account-id
                (let [new-investor (investor-service/create-investor repository (util/parse-uuid-string account-id))]
                    (to-json-response 201 new-investor)
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

(defn- handle-get-investor [repository request]
    (if-let [investor-id (util/parse-uuid-string (last (re-find #"/investors/(.*)" (.path request))))]
        (if-let [investor (investor-service/get-investor-by-id repository investor-id)]
            (to-json-response 200 investor)
            (to-json-response 404 {:error "Investor not found!"})
        )
        (to-json-response 400 {:error "Invalid investor ID!"})
    )
)

(defn- handle-create-issuer [repository request]
    (try
        (let [request-body (parse-json-request request) account-id (get request-body :account-id nil)]
            (if account-id
                (let [new-issuer (issuer-service/create-issuer repository (util/parse-uuid-string account-id))]
                    (to-json-response 201 new-issuer)
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

(defn- handle-get-issuer [repository request]
    (if-let [issuer-id (util/parse-uuid-string (last (re-find #"/issuers/(.*)" (.path request))))]
        (if-let [issuer (issuer-service/get-issuer-by-id repository issuer-id)]
            (to-json-response 200 issuer)
            (to-json-response 404 {:error "Issuer not found!"})
        )
        (to-json-response 400 {:error "Invalid issuer ID!"})
    )
)

(defn- handle-create-loan [repository event-publisher request]
    (try
        (let [request-body (parse-json-request request)
              principal (get request-body :principal nil)
              rate (get request-body :rate nil)
              inception-date (get request-body :inception-date nil)
              term (get request-body :term nil)
              investor-id (get request-body :investor-id nil)
              issuer-id (get request-body :issuer-id nil)]
            (if (and principal rate inception-date term investor-id issuer-id)
                (if-let [investor (investor-service/get-investor-by-id repository (util/parse-uuid-string investor-id))]
                    (if-let [issuer (issuer-service/get-issuer-by-id repository (util/parse-uuid-string issuer-id))]
                        (let [new-loan (loan-service/create-loan repository principal rate inception-date term (util/parse-uuid-string investor-id) (util/parse-uuid-string issuer-id))]
                            (ports/publish-transaction-requested! event-publisher (:id new-loan) (:principal new-loan) (:account-id investor) (:account-id issuer))
                            (to-json-response 201 new-loan)
                        )
                        (to-json-response 404 {:error "Issuer not found!"})
                    )
                    (to-json-response 404 {:error "Investor not found!"})
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

(defn- handle-get-loan [repository request]
    (if-let [loan-id (util/parse-uuid-string (last (re-find #"/loans/(.*)" (.path request))))]
        (if-let [loan (loan-service/get-loan-by-id repository loan-id)]
            (to-json-response 200 loan)
            (to-json-response 404 {:error "Loan not found!"})
        )
        (to-json-response 400 {:error "Invalid loan ID!"})
    )
)

(defn create-server [port repository event-publisher]
    (let [handler (proxy [Service] []
        (apply [request]
            (let [path (.path request) method (.method request)]
                (cond
                    (and (= (str method) "POST") (= path "/investors"))
                    (handle-create-investor repository request)

                    (and (= (str method) "GET") (re-matches #"/investors/.*" path))
                    (handle-get-investor repository request)

                    (and (= (str method) "POST") (= path "/issuers"))
                    (handle-create-issuer repository request)

                    (and (= (str method) "GET") (re-matches #"/issuers/.*" path))
                    (handle-get-issuer repository request)

                    (and (= (str method) "POST") (= path "/loans"))
                    (handle-create-loan repository event-publisher request)

                    (and (= (str method) "GET") (re-matches #"/loans/.*" path))
                    (handle-get-loan repository request)

                    :else
                    (to-json-response 404 {:error "PIKA"})
                )
            )
        ))]
        (Http/serve (str ":" port) handler)
    )
)

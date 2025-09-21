(ns bank.adapters.finagle-controller
    (:require [cheshire.core :as json]
              [bank.application.account-service :as account-service])
    (:import (com.twitter.finagle Service Http)
             (com.twitter.finagle.http Request Response Status)
             (com.twitter.util Future)))

(defn- to-json-response [body status]
    (let [response (Response/apply)]
        (.put (.headerMap response) "content-type" "application/json; charset=utf-8")
        (.setContentString response (json/generate-string body))
        (.status response status)
        (Future/value response)))

(defn- handle-create-account [repository request]
    (let [new-account (account-service/create-account repository)]
        (to-json-response new-account (Status/fromCode 201))))

(defn- handle-get-account [repository request]
    (let [account-id (java.util.UUID/fromString (last (re-find #"/accounts/(.*)" (.path request))))]
        (if-let [account (account-service/get-account-by-id repository account-id)]
            (to-json-response account (Status/fromCode 200))
            (to-json-response {:error "Account not found!"} (Status/fromCode 404)))))

(defn create-server [port repository]
    (let [handler (proxy [Service] []
        (apply [request]
            (let [path (.path request) method (.method request)]
                (cond
                    (and (= (str method) "POST") (= path "/accounts"))
                    (handle-create-account repository request)

                    (and (= (str method) "GET") (re-matches #"/accounts/.*" path))
                    (handle-get-account repository request)

                    :else
                    (to-json-response {} (Status/fromCode 404))))))]
    (Http/serve (str ":" port) handler)))
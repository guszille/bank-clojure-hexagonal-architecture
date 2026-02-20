(ns bank.adapters.kafka-consumer
    (:require [cheshire.core :as json]
              [bank.application.account-service :as account-service]
              [bank.application.transaction-service :as transaction-service]
              [bank.ports.event-publisher :as ports]
              [bank.domain.events :as domain]
    )
    (:import [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords]
             [org.apache.kafka.common.serialization StringDeserializer]
    )
)

(defn- parse-bigdec-fields [m]
    (into {}
        (map (fn [[k v]] [k (cond
            (instance? java.math.BigDecimal v) v
            (and (string? v) (.endsWith v "M")) (bigdec (subs v 0 (dec (count v))))
            :else v
        )]))
        m
    )
)

(defn- parse-json-record [record]
    (let [value (json/parse-string (.value record) true)]
        (parse-bigdec-fields value)
    )
)

(defn- parse-uuid-string [value]
    {:pre [(string? value)]}
    (if (not (clojure.string/blank? value)) (java.util.UUID/fromString value) nil)
)

(defn handle-transaction-requested! [repository event-publisher record-value]
    (let [event-id (parse-uuid-string (get record-value :id nil))
          value (get record-value :value nil)
          source-account-id (parse-uuid-string (get record-value :source-account-id nil))
          destination-account-id (parse-uuid-string (get record-value :destination-account-id nil))
          event (domain/create-transaction-request-event event-id value source-account-id destination-account-id)]
        (try
            (let [update-account-handler-1 (account-service/update-account-balance repository source-account-id (* -1 value))
                  update-account-handler-2 (account-service/update-account-balance repository destination-account-id value)
                  new-transaction (transaction-service/create-transaction repository "transfer" value source-account-id destination-account-id)]
                (update-account-handler-1) ;; Commit update 1.
                (update-account-handler-2) ;; Commit update 2.

                (ports/publish-transaction-approved! event-publisher event-id)
            )
            (catch Exception e
                (println "Ledger consumer failed to create transaction:" (.getMessage e))

                (ports/publish-transaction-denied! event-publisher event-id)
            )
        )
    )
)

(defn create-listener [repository event-publisher]
    (let [bootstrap-servers (or (System/getenv "KAFKA_BOOTSTRAP_SERVERS") "localhost:9092")
          config {"bootstrap.servers" bootstrap-servers "group.id" "ledger-service" "auto.offset.reset" "earliest" "key.deserializer" StringDeserializer "value.deserializer" StringDeserializer}
          listener (doto (KafkaConsumer. config) (.subscribe ["Transaction.requested"]))]
        (future
            (while true
                (try
                    (let [records (.poll listener (java.time.Duration/ofMillis 1000))]
                        (doseq [record (iterator-seq (.iterator records))]
                            (try
                                (let [record-value (parse-json-record record)]
                                    (println "Ledger consumer received \"Transaction.requested\":" record)

                                    (handle-transaction-requested! repository event-publisher record-value)
                                )
                                (catch Exception e
                                    (println "Ledger consumer failed to parse/process record:" (.getMessage e))
                                )
                            )
                        )
                        (.commitSync listener)
                    )
                    (catch Exception e
                        (println "Ledger consumer loop error:" (.getMessage e))
                    )
                )
            )
        )
    )
)

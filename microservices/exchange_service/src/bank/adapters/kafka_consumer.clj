(ns bank.adapters.kafka-consumer
    (:require [cheshire.core :as json]
              [bank.application.loan-service :as loan-service]
              [bank.domain.events :as domain]
              [bank.adapters.util :as util]
    )
    (:import [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords]
             [org.apache.kafka.common.serialization StringDeserializer]
    )
)

(defn handle-transaction-approved! [repository record-value]
    (let [event-id (util/parse-uuid-string (get record-value :id nil))
          event (domain/create-transaction-approval-event event-id)]
        (try
            (let [update-loan-handler (loan-service/update-loan-status repository event-id "approved")]
                (update-loan-handler) ;; Commit update.

                (println "Exchange consumer approved the loan:" event-id)
            )
            (catch Exception e
                (println "Exchange consumer failed to update loan:" (.getMessage e))
            )
        )
    )
)

(defn handle-transaction-denied! [repository record-value]
    (let [event-id (util/parse-uuid-string (get record-value :id nil))
          event (domain/create-transaction-approval-event event-id)]
        (try
            (let [update-loan-handler (loan-service/update-loan-status repository event-id "denied")]
                (update-loan-handler) ;; Commit update.

                (println "Exchange consumer denied the loan:" event-id)
            )
            (catch Exception e
                (println "Exchange consumer failed to update loan:" (.getMessage e))
            )
        )
    )
)

(defn create-listener [repository]
    (let [bootstrap-servers (or (System/getenv "KAFKA_BOOTSTRAP_SERVERS") "localhost:9092")
          config {"bootstrap.servers" bootstrap-servers "group.id" "exchange-service" "auto.offset.reset" "earliest" "key.deserializer" StringDeserializer "value.deserializer" StringDeserializer}
          listener (doto (KafkaConsumer. config) (.subscribe ["Transaction.approved" "Transaction.denied"]))]
        (future
            (while true
                (try
                    (let [records (.poll listener (java.time.Duration/ofMillis 1000))]
                        (doseq [record (iterator-seq (.iterator records))]
                            (try
                                (let [record-topic (.topic record)
                                      record-value (json/parse-string (.value record) true)]
                                    (cond
                                        (= record-topic "Transaction.approved")
                                        (do (println "Exchange consumer received \"Transaction.approved\":" record)
                                            (handle-transaction-approved! repository record-value)
                                        )

                                        (= record-topic "Transaction.denied")
                                        (do (println "Exchange consumer received \"Transaction.denied\":" record)
                                            (handle-transaction-denied! repository record-value)
                                        )

                                        :else
                                        (println "Exchange consumer received an unknown record topic:" record)
                                    )
                                )
                                (catch Exception e
                                    (println "Exchange consumer failed to parse/process record:" (.getMessage e))
                                )
                            )
                        )
                        (.commitSync listener)
                    )
                    (catch Exception e
                        (println "Exchange consumer loop error:" (.getMessage e))
                    )
                )
            )
        )
    )
)

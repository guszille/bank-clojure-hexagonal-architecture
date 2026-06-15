(ns bank.adapters.outbox-relay
    (:require [bank.ports.repository :as ports])
    (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
             [org.apache.kafka.common.serialization StringSerializer]
    )
)

;; Drains the transactional outbox to Kafka. Producers commit outbox rows atomically with their business change; this relay
;; polls for unsent rows and publishes them at-least-once -- a row is marked sent only after the broker acks, so a crash in
;; between re-sends it. Consumers are idempotent, so re-sends are safe.
(defn create-relay [repository]
    (let [bootstrap-servers (or (System/getenv "KAFKA_BOOTSTRAP_SERVERS") "localhost:9092")
          config {"bootstrap.servers" bootstrap-servers "acks" "all" "key.serializer" StringSerializer "value.serializer" StringSerializer}
          producer (KafkaProducer. config)]
        (future
            (while true
                (try
                    (doseq [row (ports/get-unsent-outbox-events repository)]
                        (let [record (ProducerRecord. (:topic row) (:event_key row) (:payload row))]
                            (.get (.send producer record)) ;; Block until the broker acks before marking sent.
                            (ports/mark-outbox-sent! repository (:id row))

                            (println "Outbox relay published" (:topic row) (str (:id row)))
                        )
                    )
                    (catch Exception e
                        (println "Outbox relay error:" (.getMessage e))
                    )
                )
                (Thread/sleep 500)
            )
        )
    )
)

(ns bank.adapters.kafka-producer
    (:require [cheshire.core :as json]
              [bank.ports.event-publisher :as ports]
              [bank.domain.events :as domain]
              [bank.adapters.util :as util]
    )
    (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
             [org.apache.kafka.common.serialization StringSerializer]
    )
)

(defrecord KafkaEventPublisher [producer]
    ports/EventPublisher

    (publish-transaction-requested! [this event-id value source-account-id destination-account-id]
        (let [transaction-event (domain/create-transaction-request-event event-id value source-account-id destination-account-id)
              record-key (str event-id)
              record-value (json/generate-string (util/compose-bigdec-fields transaction-event))
              record (ProducerRecord. "Transaction.requested" record-key record-value)]
            (.send producer record)

            (println "Exchange producer published \"Transaction.requested\":" record)
        )
    )
)

(defn create-publisher []
    (let [bootstrap-servers (or (System/getenv "KAFKA_BOOTSTRAP_SERVERS") "localhost:9092")
          config {"bootstrap.servers" bootstrap-servers "acks" "all" "key.serializer" StringSerializer "value.serializer" StringSerializer}
          producer (KafkaProducer. config)]
        (->KafkaEventPublisher producer)
    )
)

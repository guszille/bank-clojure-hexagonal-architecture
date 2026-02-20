(ns bank.adapters.kafka-producer
    (:require [cheshire.core :as json]
              [bank.ports.event-publisher :as ports]
              [bank.domain.events :as domain]
    )
    (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
             [org.apache.kafka.common.serialization StringSerializer]
    )
)

(defrecord KafkaEventPublisher [producer]
    ports/EventPublisher

    (publish-transaction-approved! [this event-id]
        (let [transaction-event (domain/create-transaction-approval-event event-id)
              record-key (str event-id)
              record-value (json/generate-string transaction-event)
              record (ProducerRecord. "Transaction.approved" record-key record-value)]
            (.send producer record)

            (println "Ledger producer published \"Transaction.approved\":" record)
        )
    )

    (publish-transaction-denied! [this event-id]
        (let [transaction-event (domain/create-transaction-denial-event event-id)
              record-key (str event-id)
              record-value (json/generate-string transaction-event)
              record (ProducerRecord. "Transaction.denied" record-key record-value)]
            (.send producer record)

            (println "Ledger producer published \"Transaction.denied\":" record)
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

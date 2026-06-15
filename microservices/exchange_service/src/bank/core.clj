(ns bank.core
    (:require [bank.adapters.postgres-repository :as pg-repo]
              [bank.adapters.kafka-consumer :as consumer]
              [bank.adapters.outbox-publisher :as outbox-publisher]
              [bank.adapters.outbox-relay :as outbox-relay]
              [bank.adapters.finagle-controller :as controller]
    )
    (:import (com.twitter.util Await)
    )
)

(defn -main [& args]
    (let [port (or (System/getenv "PORT") 3002)
          repository (pg-repo/create-postgres-repository)
          publisher (outbox-publisher/create-publisher)
          server (controller/create-server port repository publisher)]

        (println "[Exchange service] Outbox relay starting...")
        (outbox-relay/create-relay repository)

        (println "[Exchange service] Kafka listener starting...")
        (consumer/create-listener repository)

        (println "[Exchange service] Finagle server starting...")
        (Await/ready server)
    )
)

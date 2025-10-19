(ns bank.core
    (:require [bank.adapters.in-memory-repository :as im-repo]
              [bank.adapters.postgres-repository :as pg-repo]
              [bank.adapters.finagle-controller :as controller]
    )
    (:import (com.twitter.util Await)
    )
)

(defn -main [& args]
    (let [repository (pg-repo/create-postgres-repository)
          server (controller/create-server 8080 repository)]
        (println "Finagle server starting on port 8080.")

        (Await/ready server)
    )
)
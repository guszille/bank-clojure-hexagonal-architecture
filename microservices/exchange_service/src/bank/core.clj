(ns bank.core
    (:require [bank.adapters.postgres-repository :as pg-repo]
              [bank.adapters.finagle-controller :as controller]
    )
    (:import (com.twitter.util Await)
    )
)

(defn -main [& args]
    (let [port (or (System/getenv "PORT") 3002)
          repository (pg-repo/create-postgres-repository)
          server (controller/create-server port repository)]
        (println "[Exchange service] Finagle server starting on port" port)

        (Await/ready server)
    )
)
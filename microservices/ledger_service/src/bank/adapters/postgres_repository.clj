(ns bank.adapters.postgres-repository
    (:require [next.jdbc :as jdbc]
              [next.jdbc.result-set :as jdbc-rs]
              [honey.sql :as sql]
              [honey.sql.helpers :as sql-helpers]
              [bank.ports.repository :as ports]
              [bank.domain.account :as account-domain]
              [bank.domain.transaction :as transaction-domain]
    )
)

(def db-config {
    :dbtype   "postgresql"
    :dbname   (System/getenv "DB_NAME")
    :host     (or (System/getenv "DB_HOST") "localhost")
    :port     (Integer/parseInt (or (System/getenv "DB_PORT") "5432"))
    :user     (System/getenv "DB_USER")
    :password (System/getenv "DB_PASSWORD")
})

(def base-ds (jdbc/get-datasource db-config))
(def ds (jdbc/with-options base-ds {:builder-fn jdbc-rs/as-unqualified-lower-maps}))

(defn- snake-keys->kebab [m]
    (into {}
        (map (fn [[k v]]
            [(-> k name (clojure.string/replace "_" "-") keyword) v]
        ))
    m)
)

(defn- row->account [row]
    (when row (account-domain/map->Account row))
)

(defn- row->transaction [row]
    (when row (-> row snake-keys->kebab transaction-domain/map->Transaction))
)

(defrecord PostgresRepository [ds]
    ports/Repository

    (insert! [this table item]
        (case table
            :accounts (do
                (let [query (-> (sql-helpers/insert-into :accounts)
                                (sql-helpers/values [(select-keys item [:id :number :balance])])
                                (sql/format)
                            )]
                    (jdbc/execute! ds query)
                    item
                )
            )
            :transactions (do
                (let [query (-> (sql-helpers/insert-into :transactions)
                                (sql-helpers/values [(select-keys item [:id :type :value :source-account-id :destination-account-id])])
                                (sql/format)
                            )]
                    (jdbc/execute! ds query)
                    item
                )
            )
            (throw (ex-info "Can't insert items into table!" {:table table}))
        )
    )
    (update! [this table id args]
        (let [query (-> (sql-helpers/update table)
                        (sql-helpers/set args)
                        (sql-helpers/where [:= :id id])
                        (sql/format)
                    )]
            (jdbc/execute! ds query)

            (ports/get-by-id this table id)
        )
    )
    (delete! [this table id]
        (let [query (-> (sql-helpers/delete-from table)
                        (sql-helpers/where [:= :id id])
                        (sql/format)
                    )]
            (jdbc/execute! ds query)
        )
    )
    (get-by-id [this table id]
        (let [query (-> (sql-helpers/select :*)
                        (sql-helpers/from table)
                        (sql-helpers/where [:= :id id])
                        (sql/format)
                    )
              result (first (jdbc/execute! ds query))]
            (case table
                :accounts (do (when result (row->account result)))
                :transactions (do (when result (row->transaction result)))
            )
        )
    )
)

(defn create-postgres-repository []
    (->PostgresRepository ds)
)

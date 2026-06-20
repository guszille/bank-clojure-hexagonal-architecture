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
    :dbtype "postgresql"
    :dbname (System/getenv "DB_NAME")
    :host (or (System/getenv "DB_HOST") "localhost")
    :port (Integer/parseInt (or (System/getenv "DB_PORT") "5432"))
    :user (System/getenv "DB_USER")
    :password (System/getenv "DB_PASSWORD")})

(def rs-opts {:builder-fn jdbc-rs/as-unqualified-lower-maps})
(def base-ds (jdbc/get-datasource db-config))
(def ds (jdbc/with-options base-ds rs-opts))

(defn- snake-keys->kebab [m]
    (into {}
        (map (fn [[k v]]
            [(-> k name (clojure.string/replace "_" "-") keyword) v]
        ))
        m
    )
)

(defn- row->account [row]
    (when row (account-domain/map->Account row))
)

(defn- row->transaction [row]
    (when row (-> row snake-keys->kebab transaction-domain/map->Transaction))
)

(defrecord PostgresRepository [ds]
    ports/Repository

    (with-tx [this f]
        ;; Runs f against a repository bound to a single transacted connection, so every port call inside f shares one DB
        ;; transaction (all-or-nothing). The transacted connection is re-wrapped with rs-opts so result-set keys stay
        ;; unqualified.
        (jdbc/with-transaction [tx ds]
            (f (->PostgresRepository (jdbc/with-options tx rs-opts)))
        )
    )
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
            :processed-events (do
                (let [query (-> (sql-helpers/insert-into :processed-events)
                                (sql-helpers/values [(select-keys item [:id :outcome])])
                                (sql/format)
                            )]
                    (jdbc/execute! ds query)
                    item
                )
            )
            :outbox (do
                (let [query (-> (sql-helpers/insert-into :outbox)
                                (sql-helpers/values [(select-keys item [:id :topic :event-key :payload])])
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
                :processed-events result
            )
        )
    )
    (get-all [this table]
        (let [query (-> (sql-helpers/select :*)
                        (sql-helpers/from table)
                        (sql/format)
                    )
              rows (jdbc/execute! ds query)]
            (case table
                :accounts (mapv row->account rows)
                :transactions (mapv row->transaction rows)
            )
        )
    )
    (get-for-update [this table id]
        (let [query (-> (sql-helpers/select :*)
                        (sql-helpers/from table)
                        (sql-helpers/where [:= :id id])
                        (sql-helpers/for :update)
                        (sql/format)
                    )
              result (first (jdbc/execute! ds query))]
            (case table
                :accounts (when result (row->account result))
                :transactions (when result (row->transaction result))
            )
        )
    )
    (get-next-account-number [this]
        (let [result (first (jdbc/execute! ds ["SELECT TO_CHAR(nextval('account_number_sequence'), 'FM00000') AS number"]))]
            (:number result)
        )
    )
    (get-unsent-outbox-events [this]
        (jdbc/execute! ds ["SELECT * FROM outbox WHERE sent_at IS NULL ORDER BY created_at"])
    )
    (mark-outbox-sent! [this id]
        (jdbc/execute! ds ["UPDATE outbox SET sent_at = now() WHERE id = ?" id])
    )
)

(defn create-postgres-repository []
    (->PostgresRepository ds)
)

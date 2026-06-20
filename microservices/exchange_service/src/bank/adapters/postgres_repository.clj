(ns bank.adapters.postgres-repository
    (:require [next.jdbc :as jdbc]
              [next.jdbc.result-set :as jdbc-rs]
              [honey.sql :as sql]
              [honey.sql.helpers :as sql-helpers]
              [bank.ports.repository :as ports]
              [bank.domain.investor :as investor-domain]
              [bank.domain.issuer :as issuer-domain]
              [bank.domain.loan :as loan-domain]
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

(defn- row->investor [row]
    (when row (-> row snake-keys->kebab investor-domain/map->Investor))
)

(defn- row->issuer [row]
    (when row (-> row snake-keys->kebab issuer-domain/map->Issuer))
)

(defn- row->loan [row]
    (when row (-> row snake-keys->kebab loan-domain/map->Loan))
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
            :investors (do
                (let [query (-> (sql-helpers/insert-into :investors)
                                (sql-helpers/values [(select-keys item [:id :account-id])])
                                (sql/format)
                            )]
                    (jdbc/execute! ds query)
                    item
                )
            )
            :issuers (do
                (let [query (-> (sql-helpers/insert-into :issuers)
                                (sql-helpers/values [(select-keys item [:id :account-id])])
                                (sql/format)
                            )]
                    (jdbc/execute! ds query)
                    item
                )
            )
            :loans (do
                (let [query (-> (sql-helpers/insert-into :loans)
                                (sql-helpers/values [(select-keys item [:id :principal :rate :inception-date :term :investor-id :issuer-id :status])])
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
                :investors (do (when result (row->investor result)))
                :issuers (do (when result (row->issuer result)))
                :loans (do (when result (row->loan result)))
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
                :investors (mapv row->investor rows)
                :issuers (mapv row->issuer rows)
                :loans (mapv row->loan rows)
            )
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

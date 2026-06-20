(ns bank.application.account-service-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.application.account-service :as account-service]
              [bank.ports.repository :as ports]
    )
)

;; An in-memory Repository over a single :accounts table, enough to exercise the create / list use cases.
;; get-next-account-number hands out a fresh zero-padded number per call, mirroring the Postgres sequence.
(defn- fake-repo []
    (let [accounts (atom {})
          counter (atom 0)]
        {:accounts accounts
         :repo (reify ports/Repository
             (insert! [this table item]
                 (swap! accounts assoc (:id item) item)
                 item
             )
             (get-by-id [this table id]
                 (get @accounts id)
             )
             (get-all [this table]
                 (vec (vals @accounts))
             )
             (get-next-account-number [this]
                 (format "%05d" (swap! counter inc))
             )
         )}
    )
)

(deftest get-all-accounts-returns-every-created-account
    (let [{:keys [repo accounts]} (fake-repo)]
        (testing "an empty repository lists no accounts"
            (is (= [] (account-service/get-all-accounts repo)))
        )
        (let [a (account-service/create-account repo)
              b (account-service/create-account repo)
              listed (account-service/get-all-accounts repo)]
            (testing "every created account is returned"
                (is (= 2 (count listed)))
                (is (= #{(:id a) (:id b)} (set (map :id listed))))
            )
            (testing "new accounts start at a zero balance"
                (is (every? #(= (bigdec 0.00) (:balance %)) listed))
            )
        )
    )
)

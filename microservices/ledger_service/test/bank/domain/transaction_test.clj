(ns bank.domain.transaction-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.domain.transaction :refer [create-transaction]]
    )
)

(def valid-id (java.util.UUID/randomUUID))
(def valid-source-id (java.util.UUID/randomUUID))
(def valid-destination-id (java.util.UUID/randomUUID))
(def valid-value 50.00M)

(deftest create-transaction-test
    (testing "valid deposit is created successfully"
        (let [transaction (create-transaction valid-id "deposit" valid-value nil valid-destination-id)]
            (is (= valid-id (:id transaction)))
            (is (= "deposit" (:type transaction)))
            (is (= valid-value (:value transaction)))
            (is (nil? (:source-account-id transaction)))
            (is (= valid-destination-id (:destination-account-id transaction)))
        )
    )

    (testing "valid withdrawal is created successfully"
        (let [transaction (create-transaction valid-id "withdrawal" valid-value valid-source-id nil)]
            (is (= "withdrawal" (:type transaction)))
            (is (= valid-source-id (:source-account-id transaction)))
            (is (nil? (:destination-account-id transaction)))
        )
    )

    (testing "valid transfer is created successfully"
        (let [transaction (create-transaction valid-id "transfer" valid-value valid-source-id valid-destination-id)]
            (is (= "transfer" (:type transaction)))
            (is (= valid-source-id (:source-account-id transaction)))
            (is (= valid-destination-id (:destination-account-id transaction)))
        )
    )

    (testing "invalid ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid ID"
                (create-transaction "not-a-uuid" "deposit" valid-value nil valid-destination-id)
            )
        )
    )

    (testing "invalid transaction type throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid transaction type"
                (create-transaction valid-id "payment" valid-value nil valid-destination-id)
            )
        )
    )

    (testing "zero value throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid transaction value"
                (create-transaction valid-id "deposit" 0.00M nil valid-destination-id)
            )
        )
    )

    (testing "negative value throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid transaction value"
                (create-transaction valid-id "deposit" -10.00M nil valid-destination-id)
            )
        )
    )

    (testing "withdrawal with missing source account ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid source account ID"
                (create-transaction valid-id "withdrawal" valid-value nil nil)
            )
        )
    )

    (testing "deposit with non-nil source account ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid source account ID"
                (create-transaction valid-id "deposit" valid-value valid-source-id valid-destination-id)
            )
        )
    )

    (testing "deposit with missing destination account ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid destination account ID"
                (create-transaction valid-id "deposit" valid-value nil nil)
            )
        )
    )

    (testing "withdrawal with non-nil destination account ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid destination account ID"
                (create-transaction valid-id "withdrawal" valid-value valid-source-id valid-destination-id)
            )
        )
    )
)

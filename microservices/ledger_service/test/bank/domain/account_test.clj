(ns bank.domain.account-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.domain.account :refer [create-account update-account-balance ->Account]]
    )
)

(def valid-id (java.util.UUID/randomUUID))
(def valid-number "12345")
(def valid-balance 100.00M)

(deftest create-account-test
    (testing "valid account is created successfully"
        (let [account (create-account valid-id valid-number valid-balance)]
            (is (= valid-id (:id account)))
            (is (= valid-number (:number account)))
            (is (= valid-balance (:balance account)))
        )
    )

    (testing "invalid ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid ID"
                (create-account "not-a-uuid" valid-number valid-balance)
            )
        )
    )

    (testing "invalid account number throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid account number"
                (create-account valid-id "123" valid-balance)
            )
        )
    )

    (testing "negative balance throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid account balance"
                (create-account valid-id valid-number -1.00M)
            )
        )
    )

    (testing "non-bigdecimal balance throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid account balance"
                (create-account valid-id valid-number 100.0)
            )
        )
    )
)

(deftest update-account-balance-test
    (testing "balance is updated successfully"
        (let [account (->Account valid-id valid-number 100.00M)
              updated (update-account-balance account 50.00M)]
            (is (= 150.00M (:balance updated)))
        )
    )

    (testing "balance can be decreased"
        (let [account (->Account valid-id valid-number 100.00M)
              updated (update-account-balance account -50.00M)]
            (is (= 50.00M (:balance updated)))
        )
    )

    (testing "balance going below zero throws exception"
        (let [account (->Account valid-id valid-number 10.00M)]
            (is
                (thrown-with-msg?
                    clojure.lang.ExceptionInfo
                    #"balance must be greater than or equal to zero"
                    (update-account-balance account -50.00M)
                )
            )
        )
    )

    (testing "non-Account argument throws assertion error"
        (is (thrown? AssertionError (update-account-balance {} 10.00M)))
    )

    (testing "non-bigdecimal value throws assertion error"
        (let [account (->Account valid-id valid-number 100.00M)]
            (is (thrown? AssertionError (update-account-balance account 10.0)))
        )
    )
)

(ns bank.domain.loan-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.domain.loan :refer [create-loan update-loan-status ->Loan]]
    )
)

(def valid-id (java.util.UUID/randomUUID))
(def valid-investor-id (java.util.UUID/randomUUID))
(def valid-issuer-id (java.util.UUID/randomUUID))
(def valid-principal 1000.00M)
(def valid-rate 0.05M)
(def valid-inception-date "2024-01-01")
(def valid-term 12)
(def valid-status "created")

(deftest create-loan-test
    (testing "valid loan is created successfully"
        (let [loan (create-loan valid-id valid-principal valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id valid-status)]
            (is (= valid-id (:id loan)))
            (is (= valid-principal (:principal loan)))
            (is (= valid-rate (:rate loan)))
            (is (= valid-inception-date (:inception-date loan)))
            (is (= valid-term (:term loan)))
            (is (= valid-investor-id (:investor-id loan)))
            (is (= valid-issuer-id (:issuer-id loan)))
            (is (= valid-status (:status loan)))
        )
    )

    (testing "invalid ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid ID"
                (create-loan "not-a-uuid" valid-principal valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id valid-status)
            )
        )
    )

    (testing "negative principal throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid principal"
                (create-loan valid-id -1.00M valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id valid-status)
            )
        )
    )

    (testing "non-bigdecimal principal throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid principal"
                (create-loan valid-id 1000.0 valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id valid-status)
            )
        )
    )

    (testing "negative rate throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid rate"
                (create-loan valid-id valid-principal -0.01M valid-inception-date valid-term valid-investor-id valid-issuer-id valid-status)
            )
        )
    )

    (testing "invalid inception date throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid inception date"
                (create-loan valid-id valid-principal valid-rate "not-a-date" valid-term valid-investor-id valid-issuer-id valid-status)
            )
        )
    )

    (testing "zero term throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid term"
                (create-loan valid-id valid-principal valid-rate valid-inception-date 0 valid-investor-id valid-issuer-id valid-status)
            )
        )
    )

    (testing "negative term throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid term"
                (create-loan valid-id valid-principal valid-rate valid-inception-date -1 valid-investor-id valid-issuer-id valid-status)
            )
        )
    )

    (testing "invalid investor ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid investor ID"
                (create-loan valid-id valid-principal valid-rate valid-inception-date valid-term "not-a-uuid" valid-issuer-id valid-status)
            )
        )
    )

    (testing "invalid issuer ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid issuer ID"
                (create-loan valid-id valid-principal valid-rate valid-inception-date valid-term valid-investor-id "not-a-uuid" valid-status)
            )
        )
    )

    (testing "invalid status throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid loan status"
                (create-loan valid-id valid-principal valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id "unknown")
            )
        )
    )
)

(deftest update-loan-status-test
    (testing "loan status is updated successfully from created"
        (let [loan (->Loan valid-id valid-principal valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id "created")
              updated (update-loan-status loan "approved")]
            (is (= "approved" (:status updated)))
        )
    )

    (testing "loan already approved throws exception"
        (let [loan (->Loan valid-id valid-principal valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id "approved")]
            (is
                (thrown-with-msg?
                    clojure.lang.ExceptionInfo
                    #"already approved or denied"
                    (update-loan-status loan "denied")
                )
            )
        )
    )

    (testing "loan already denied throws exception"
        (let [loan (->Loan valid-id valid-principal valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id "denied")]
            (is
                (thrown-with-msg?
                    clojure.lang.ExceptionInfo
                    #"already approved or denied"
                    (update-loan-status loan "approved")
                )
            )
        )
    )

    (testing "non-Loan argument throws assertion error"
        (is (thrown? AssertionError (update-loan-status {} "approved")))
    )

    (testing "non-string new-status throws assertion error"
        (let [loan (->Loan valid-id valid-principal valid-rate valid-inception-date valid-term valid-investor-id valid-issuer-id "created")]
            (is (thrown? AssertionError (update-loan-status loan 123)))
        )
    )
)

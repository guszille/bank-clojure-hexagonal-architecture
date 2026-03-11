(ns bank.domain.issuer-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.domain.issuer :refer [create-issuer]]
    )
)

(def valid-id (java.util.UUID/randomUUID))
(def valid-account-id (java.util.UUID/randomUUID))

(deftest create-issuer-test
    (testing "valid issuer is created successfully"
        (let [issuer (create-issuer valid-id valid-account-id)]
            (is (= valid-id (:id issuer)))
            (is (= valid-account-id (:account-id issuer)))
        )
    )

    (testing "invalid ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid ID"
                (create-issuer "not-a-uuid" valid-account-id)
            )
        )
    )

    (testing "invalid account ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid account ID"
                (create-issuer valid-id "not-a-uuid")
            )
        )
    )
)

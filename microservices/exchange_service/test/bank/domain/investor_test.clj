(ns bank.domain.investor-test
    (:require [clojure.test :refer [deftest is testing]]
              [bank.domain.investor :refer [create-investor]]
    )
)

(def valid-id (java.util.UUID/randomUUID))
(def valid-account-id (java.util.UUID/randomUUID))

(deftest create-investor-test
    (testing "valid investor is created successfully"
        (let [investor (create-investor valid-id valid-account-id)]
            (is (= valid-id (:id investor)))
            (is (= valid-account-id (:account-id investor)))
        )
    )

    (testing "invalid ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid ID"
                (create-investor "not-a-uuid" valid-account-id)
            )
        )
    )

    (testing "invalid account ID throws exception"
        (is
            (thrown-with-msg?
                clojure.lang.ExceptionInfo
                #"invalid account ID"
                (create-investor valid-id "not-a-uuid")
            )
        )
    )
)

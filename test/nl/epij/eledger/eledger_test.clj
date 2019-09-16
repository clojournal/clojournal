(ns nl.epij.eledger.eledger-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [nl.epij.eledger.core :as ledger]))

(stest/instrument `ledger/balance)

(deftest journal-generation
  (let [results (stest/check `ledger/balance)]
    (doseq [result results
            :let [return  (get result :clojure.spec.test.check/ret)
                  passed? (:pass? return)]]
      (is passed?)
      (is (= (when-not passed? result) nil)))))

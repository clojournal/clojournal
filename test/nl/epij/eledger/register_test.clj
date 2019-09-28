(ns nl.epij.eledger.register-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [nl.epij.eledger.api :as api]))

(deftest register-parsing
  (let [results (stest/check [`api/eledger] #_{:clojure.spec.test.check/opts {:num-tests 1}})]
    (doseq [result results
            :let [return  (get result :clojure.spec.test.check/ret)
                  passed? (:pass? return)]]
      (is passed?)

      (when-not passed?
        (is (= (keys result) []))

        (is (= (get-in return [:fail]) []))

        (is (= (get result :failure) []))

        ))))

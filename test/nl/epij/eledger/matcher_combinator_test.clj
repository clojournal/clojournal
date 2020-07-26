(ns nl.epij.eledger.matcher-combinator-test
  (:require [clojure.test :refer :all]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]))

(deftest test-matching-with-explicit-matchers
  (is (match? (m/equals 37) (+ 29 8)))
  (is (match? (m/regex #"fox") "The quick brown fox jumps over the lazy dog")))

(deftest test-matching-maps
  ;; A map is interpreted as an `embeds` matcher, which ignores
  ;; un-specified keys
  (is (match? {:name/first "Alfredo"
               :age        5}
              {:name/first  "Alfredo"
               :age         8
               :name/last   "da Rocha Viana"
               :name/suffix "Jr."})))

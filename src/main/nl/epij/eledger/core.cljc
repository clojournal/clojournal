(ns nl.epij.eledger.core
  (:require [nl.epij.eledger :as eledger]
            [tick.alpha.api :as t]
            [clojure.spec.alpha :as s]
            [nl.epij.eledger.journal :as journal]
            [clojure.test.check.generators :as gen]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(defn balance
  [txs]
  (let [sample (journal/anti-corrupt txs)
        result (sh/sh "ledger" "source" "-f" "-" "--permissive" :in sample)]
    (assoc result :journal sample)))

(s/fdef balance
        :args (s/cat :transactions ::eledger/transactions)
        :ret (s/keys :req-un [::exit]))

(comment

  (gen/sample (s/gen ::eledger/transaction))

  (balance (gen/sample (s/gen ::eledger/transaction) 100))

  (s/exercise-fn `balance 1)

  (clojure.pprint/pprint samp)

  )

(ns nl.epij.eledger.journal-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [nl.epij.eledger.api :as api]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [nl.epij.eledger :as eledger]
            [clojure.string :as str]))

(deftest journal-generation
  (is (= (api/journal [{::eledger/date           "2019-06-01"
                        ::eledger/transaction-id #uuid "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"
                        ::eledger/payee          "Mister Shawarma"
                        ::eledger/postings       [{::eledger/account :expenses/food
                                                   ::eledger/amount  "R$ 20"}
                                                  {::eledger/account :assets/cash}]}
                       {::eledger/date     #time/date "2019-07-01"
                        ::eledger/payee    "Interactive Brokers"
                        ::eledger/postings [{::eledger/account :assets/stocks
                                             ::eledger/amount  "USD 1336"}
                                            {::eledger/account "Expenses:Commissions"
                                             ::eledger/amount  "USD 1"}
                                            {::eledger/account :assets/checking}]}])
         (str/triml "
2019-06-01 (960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc) Mister Shawarma
  ~:expenses/food  R$ 20
  ~:assets/cash

2019-07-01 Interactive Brokers
  ~:assets/stocks  USD 1336
  Expenses:Commissions  USD 1
  ~:assets/checking
"))))

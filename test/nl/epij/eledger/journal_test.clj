(ns nl.epij.eledger.journal-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [nl.epij.eledger.api :as api]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [nl.epij.eledger :as eledger]
            [nl.epij.eledger.virtual :as virtual]
            [clojure.string :as str]))

(deftest journal-generation
  (is (= (api/journal [{::eledger/date           "2019-06-01"
                        ::eledger/transaction-id #uuid "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"
                        ::eledger/payee          "Mister Shawarma"
                        ::eledger/status         ::eledger/cleared
                        ::eledger/postings       [{::eledger/account :expenses/food
                                                   ::eledger/amount  "R$ 20"}
                                                  {::eledger/account :assets/cash}]}
                       {::eledger/date     #time/date "2019-07-01"
                        ::eledger/payee    "Interactive Brokers"
                        ::eledger/postings [{::eledger/account :assets/stocks
                                             ::eledger/amount  "USD 1336"}
                                            {::eledger/account "Expenses:Commissions"
                                             ::eledger/amount  "USD 1"}
                                            {::eledger/account :assets/checking}]}]
                      {::eledger/prices [{::eledger/date      "2019-01-01"
                                          ::eledger/commodity "R$"
                                          ::eledger/price     "€ 0.25"}
                                         {::eledger/date      "2019-01-01"
                                          ::eledger/commodity "$"
                                          ::eledger/price     "€ 0.9"}]})
         (str/triml "
P 2019-01-01 R$ € 0.25
P 2019-01-01 $ € 0.9


2019-06-01 * (960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc) Mister Shawarma
  ~:expenses/food  R$ 20
  ~:assets/cash

2019-07-01 Interactive Brokers
  ~:assets/stocks  USD 1336
  Expenses:Commissions  USD 1
  ~:assets/checking
")))

  (testing "virtual postings"
    (is (= (-> (api/journal [{::eledger/date     "2019-06-01"
                              ::eledger/payee    "Mister Shawarma"
                              ::eledger/postings [{::eledger/account :funds/food
                                                   ::eledger/amount  "R$ 20"
                                                   ::eledger/virtual ::virtual/balanced}
                                                  {::eledger/account :assets/cash
                                                   ::eledger/amount  "R$ -20"
                                                   ::eledger/virtual ::virtual/balanced}
                                                  {::eledger/account "Funds:Shawarma Time!"
                                                   ::eledger/amount  "R$ 1337"
                                                   ::eledger/virtual ::virtual/unbalanced}]}
                             ])
               str/triml)
           (str/triml "
2019-06-01 Mister Shawarma
  [~:funds/food]  R$ 20
  [~:assets/cash]  R$ -20
  (Funds:Shawarma Time!)  R$ 1337
")))))



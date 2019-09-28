(ns nl.epij.eledger.register-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [nl.epij.eledger.api :as api]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [nl.epij.eledger :as eledger]))

(stest/instrument `api/eledger)

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

        )))

  (is (= (::eledger/line-items (api/eledger [{::eledger/date           "2019-06-01"
                                              ::eledger/transaction-id #uuid "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"
                                              ::eledger/payee          "Mister Shawarma"
                                              ::eledger/postings       [{::eledger/account :expenses/food
                                                                         ::eledger/amount  "R$ 20"}
                                                                        {::eledger/account :assets/cash}]}
                                             {::eledger/date     #time/date "2019-07-01"
                                              ::eledger/payee    :foo.bar.payee/interactive-brokers
                                              ::eledger/postings [{::eledger/account :assets/stocks
                                                                   ::eledger/amount  "$ 1336"}
                                                                  {::eledger/account "Expenses:Commissions"
                                                                   ::eledger/amount  "$ 1"}
                                                                  {::eledger/account :assets/checking}]}]
                                            ::eledger/edn-register
                                            {}))
         [#:nl.epij.eledger.line-item{:account        :expenses/food
                                      :amount         #:nl.epij.eledger.monetary-amount{:commodity "R$"
                                                                                        :value     "20"}
                                      :commodity      "R$"
                                      :date           #time/date "2019-06-01"
                                      :payee          "Mister Shawarma"
                                      :transaction-id "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"}
          #:nl.epij.eledger.line-item{:account        :assets/cash
                                      :amount         #:nl.epij.eledger.monetary-amount{:commodity "R$"
                                                                                        :value     "-20"}
                                      :commodity      "R$"
                                      :date           #time/date "2019-06-01"
                                      :payee          "Mister Shawarma"
                                      :transaction-id "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"}
          #:nl.epij.eledger.line-item{:account   :assets/stocks
                                      :amount    #:nl.epij.eledger.monetary-amount{:commodity "$"
                                                                                   :value     "1336"}
                                      :commodity "$"
                                      :date      #time/date "2019-07-01"
                                      :payee     :foo.bar.payee/interactive-brokers}
          #:nl.epij.eledger.line-item{:account   "Expenses:Commissions"
                                      :amount    #:nl.epij.eledger.monetary-amount{:commodity "$"
                                                                                   :value     "1"}
                                      :commodity "$"
                                      :date      #time/date "2019-07-01"
                                      :payee     :foo.bar.payee/interactive-brokers}
          #:nl.epij.eledger.line-item{:account   :assets/checking
                                      :amount    #:nl.epij.eledger.monetary-amount{:commodity "$"
                                                                                   :value     "-1337"}
                                      :commodity "$"
                                      :date      #time/date "2019-07-01"
                                      :payee     :foo.bar.payee/interactive-brokers}]))

  (is (= (::eledger/line-items (api/eledger [{::eledger/date           "2019-06-01"
                                              ::eledger/transaction-id #uuid "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"
                                              ::eledger/payee          "Mister Shawarma"
                                              ::eledger/postings       [{::eledger/account :expenses/food
                                                                         ::eledger/amount  "R$ 20"}
                                                                        {::eledger/account :assets/cash}]}
                                             {::eledger/date     #time/date "2019-07-01"
                                              ::eledger/payee    :foo.bar.payee/interactive-brokers
                                              ::eledger/postings [{::eledger/account :assets/stocks
                                                                   ::eledger/amount  "$ 1336"}
                                                                  {::eledger/account "Expenses:Commissions"
                                                                   ::eledger/amount  "$ 1"}
                                                                  {::eledger/account :assets/checking}]}]
                                            ::eledger/edn-register
                                            {::eledger/ledger-options {:exchange "€"
                                                                       :price-db "/Users/pepijn/boekhouding/prices.txt"}}))
         [#:nl.epij.eledger.line-item{:account               :expenses/food
                                      :amount                #:nl.epij.eledger.monetary-amount{:commodity "R$"
                                                                                               :value     "20"}
                                      :commodity             "R$"
                                      :date                  #time/date "2019-06-01"
                                      :exchange              "€"
                                      :exchange-amount       #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "4.550171"}
                                      :exchange-total-amount #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "5"}
                                      :payee                 "Mister Shawarma"
                                      :transaction-id        "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"}
          #:nl.epij.eledger.line-item{:account               :assets/cash
                                      :amount                #:nl.epij.eledger.monetary-amount{:commodity "R$"
                                                                                               :value     "-20"}
                                      :commodity             "R$"
                                      :date                  #time/date "2019-06-01"
                                      :exchange              "€"
                                      :exchange-amount       #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "-4.550171"}
                                      :exchange-total-amount #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "0"}
                                      :payee                 "Mister Shawarma"
                                      :transaction-id        "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"}
          #:nl.epij.eledger.line-item{:account               :assets/stocks
                                      :amount                #:nl.epij.eledger.monetary-amount{:commodity "$"
                                                                                               :value     "1336"}
                                      :commodity             "$"
                                      :date                  #time/date "2019-07-01"
                                      :exchange              "€"
                                      :exchange-amount       #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "1183.656329"}
                                      :exchange-total-amount #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "1184"}
                                      :payee                 :foo.bar.payee/interactive-brokers}
          #:nl.epij.eledger.line-item{:account               "Expenses:Commissions"
                                      :amount                #:nl.epij.eledger.monetary-amount{:commodity "$"
                                                                                               :value     "1"}
                                      :commodity             "$"
                                      :date                  #time/date "2019-07-01"
                                      :exchange              "€"
                                      :exchange-amount       #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "0.88597"}
                                      :exchange-total-amount #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                               :value     "1185"}
                                      :payee                 :foo.bar.payee/interactive-brokers}
          #:nl.epij.eledger.line-item{:account         :assets/checking
                                      :amount          #:nl.epij.eledger.monetary-amount{:commodity "$"
                                                                                         :value     "-1337"}
                                      :commodity       "$"
                                      :date            #time/date "2019-07-01"
                                      :exchange        "€"
                                      :exchange-amount #:nl.epij.eledger.monetary-amount{:commodity "€"
                                                                                         :value     "-1184.542299"}
                                      :payee           :foo.bar.payee/interactive-brokers}])))

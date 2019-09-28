(ns nl.epij.eledger.journal
  (:require [nl.epij.eledger :as eledger]
            [clojure.string :as str]))

(defn anti-corrupt
  [transactions options]
  (let [{:keys [::eledger/prices]} options
        prices (for [price prices
                     :let [{:keys [::eledger/date ::eledger/commodity ::eledger/price]} price]]
                 (str "P " date " " commodity " " price))
        all    (for [transaction transactions
                     :let [{:keys [::eledger/date ::eledger/transaction-id ::eledger/payee ::eledger/memo ::eledger/postings]} transaction
                           transaction-id (if transaction-id
                                            (str " (" transaction-id ")")
                                            "")
                           tx-postings    (for [posting postings
                                                :let [{:keys [::eledger/account
                                                              ::eledger/amount
                                                              ::eledger/balance
                                                              ::eledger/memo]} posting
                                                      memo              (if memo (str "  ;" memo "\n") "")
                                                      account           (cond
                                                                          (keyword? account) (str "~" account)
                                                                          :else account)
                                                      balance-assertion (if balance
                                                                          (str " = " balance)
                                                                          "")]]
                                            (apply str (concat [memo "  " account]
                                                               (if amount ["  " amount] [])
                                                               [balance-assertion "\n"])))
                           payee          (cond
                                            (keyword? payee) (str "~" payee)
                                            :else payee)
                           memo           (if memo (str "  ;" memo) "")]]
                 (str/join (conj tx-postings (str date transaction-id " " payee memo "\n"))))]
    (str/join "\n" (concat prices ["\n"] all))))

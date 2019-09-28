(ns nl.epij.eledger.journal
  (:require [nl.epij.eledger :as ledger]
            [clojure.string :as str]))

(defn anti-corrupt
  [transactions]
  (let [all (for [transaction transactions
                  :let [{:keys [::ledger/date ::ledger/transaction-id ::ledger/payee ::ledger/memo ::ledger/postings]} transaction
                        transaction-id (if transaction-id
                                         (str " (" transaction-id ")")
                                         "")
                        tx-postings    (for [posting postings
                                             :let [{:keys [::ledger/account
                                                           ::ledger/amount
                                                           ::ledger/balance
                                                           ::ledger/memo]} posting
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
    (str/join "\n" all)))

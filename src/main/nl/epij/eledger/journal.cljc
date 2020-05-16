(ns nl.epij.eledger.journal
  (:require [nl.epij.eledger :as eledger]
            [nl.epij.eledger.virtual :as virtual]
            [clojure.string :as str]))

(defn ledger-account
  [account virtual]
  (let [base (cond
               (keyword? account) (str "~" account)
               :else account)]
    (case virtual
      ::virtual/balanced (format "[%s]" base)
      ::virtual/unbalanced (format "(%s)" base)
      base)))

(defn anti-corrupt
  ([transactions] (anti-corrupt transactions {}))
  ([transactions options]
   (let [{:keys [::eledger/prices]} options
         prices (for [price prices
                      :let [{:keys [::eledger/date ::eledger/commodity ::eledger/price]} price]]
                  (str "P " date " " commodity " " price))
         all    (for [transaction transactions
                      :let [{:keys [::eledger/date
                                    ::eledger/transaction-id
                                    ::eledger/payee
                                    ::eledger/memo
                                    ::eledger/postings
                                    ::eledger/status]} transaction
                            transaction-id (if transaction-id
                                             (str " (" transaction-id ")")
                                             "")
                            status         (case status
                                             ::eledger/cleared " *"
                                             "")
                            tx-postings    (for [posting postings
                                                 :let [{:keys [::eledger/account
                                                               ::eledger/amount
                                                               ::eledger/balance
                                                               ::eledger/memo
                                                               ::eledger/virtual]} posting
                                                       memo              (if memo (str "  ;" memo "\n") "")
                                                       account           (ledger-account account virtual)
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
                  (str/join (conj tx-postings (str date status transaction-id " " payee memo "\n"))))]
     (str/trim (str/join "\n" (concat prices ["\n"] all))))))

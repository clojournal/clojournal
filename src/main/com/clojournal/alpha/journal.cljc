(ns com.clojournal.alpha.journal
  (:require [com.clojournal.alpha :as eledger]
            [com.clojournal.alpha.virtual :as virtual]
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
   (let [all (for [transaction transactions
                   :let [{:keys [::eledger/date
                                 ::eledger/transaction-id
                                 ::eledger/payee
                                 ::eledger/memo
                                 ::eledger/postings
                                 ::eledger/status
                                 ::eledger/commodity
                                 ::eledger/price]} transaction
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
               (cond
                 (some? price) (str "P " date " " commodity " " price "\n")
                 (string? transaction) transaction
                 :else
                 (str/join (conj tx-postings (str date status transaction-id " " payee memo "\n")))))]
     (str/join "\n" all))))

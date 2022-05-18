(ns com.clojournal.alpha.journal
  (:require [com.clojournal.alpha :as eledger]
            [com.clojournal.alpha.virtual :as virtual]
            [com.clojournal.alpha.lot :as lot]
            [com.clojournal.alpha.cost :as cost]
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
                   :let [{::eledger/keys [date
                                          transaction-id
                                          payee
                                          memo
                                          postings
                                          status
                                          commodity
                                          price]} transaction
                         transaction-id (if transaction-id
                                          (str " (" transaction-id ")")
                                          "")
                         status         (case status
                                          ::eledger/cleared " *"
                                          "")
                         tx-postings    (for [posting postings
                                              :let [{::eledger/keys [account
                                                                     amount
                                                                     balance
                                                                     memo
                                                                     virtual]} posting
                                                    memo              (if memo (str "  ;" memo "\n") "")
                                                    account           (ledger-account account virtual)
                                                    balance-assertion (if balance
                                                                        (str " = " balance)
                                                                        "")
                                                    amount'           (cond
                                                                        (map? amount)
                                                                        (let [{base ::eledger/amount
                                                                               lot  ::lot/total
                                                                               cost ::cost/total}
                                                                              amount]
                                                                          (reduce (fn [a x]
                                                                                    (str a " @@ " x))
                                                                                  (remove nil?
                                                                                          [(format "%s {{%s}}"
                                                                                                   base
                                                                                                   lot)
                                                                                           cost])))

                                                                        :else amount)]]
                                          (apply str (concat [memo "  " account]
                                                             (if amount ["  " amount'] [])
                                                             [balance-assertion "\n"])))
                         payee          (cond
                                          (keyword? payee) (str "~" payee)
                                          :else payee)
                         memo           (if memo (str "  ;" memo) "")]]
               (cond
                 (some? price) (str "P " date " " commodity " " price "\n")
                 (string? transaction) transaction
                 :else
                 (str/join (conj tx-postings (str date status transaction-id (when payee (str " " payee)) memo "\n")))))]
     (str/join "\n" all))))

(ns nl.epij.eledger.journal
  (:require [nl.epij.eledger :as ledger]
            [clojure.string :as str]))

(defn account->ledger-account
  [account]
  (-> account
      str
      (str/replace-first ":" "")
      (str/replace "/" ":")
      (str/replace "." ":")))

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
                                                   account           (account->ledger-account account)
                                                   balance-assertion (if balance
                                                                       (str "= " balance)
                                                                       "")]]
                                         (str memo "  " account "  " amount " " balance-assertion "\n"))
                        memo           (if memo (str "  ;" memo) "")]]
              (str/join (conj tx-postings (str date transaction-id " " payee memo "\n"))))]
    (str/join "\n" all)))

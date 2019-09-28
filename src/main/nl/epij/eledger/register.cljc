(ns nl.epij.eledger.register
  (:require [clojure.tools.reader :as reader]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [tick.alpha.api :as t]
            [nl.epij.eledger.payee :as payee]
            [nl.epij.eledger :as eledger]
            [nl.epij.eledger.account :as account]))

(def special-payees {"Commodities revalued" ::payee/commodities-revalued
                     "<Unspecified payee>"  ::payee/unspecified-payee})

(def special-accounts {"<Revalued>"   ::account/revalued
                       "<Adjustment>" ::account/adjustment})

(defn str-or-keyword
  [x]
  (let [value (edn/read-string x)]
    (cond
      (keyword? value) value
      :else (str x))))

(def readers
  {'time/date         t/parse
   'eledger/date      str
   ;;'ledger/commodity (comp ::local-currency/id local-currency/abbreviation->currency)
   'eledger/payee     (comp (fn [payee] (get special-payees payee payee)) str-or-keyword)
   'eledger/account   (comp (fn [account] (get special-accounts account account)) str-or-keyword)
   'eledger/line-item (fn [line-item] (->> line-item
                                           (into {} (remove (comp str/blank? str second)))))})

(defn parse
  [output-key]
  (fn [ledger-result]
    (let [{:keys [::eledger/output]} ledger-result
          line-items (->> output
                          (str/split-lines)
                          (map (partial edn/read-string {:readers readers})))]
      (assoc ledger-result output-key line-items))))

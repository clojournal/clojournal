(ns nl.epij.eledger.register
  (:require [clojure.tools.reader :as reader]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [tick.alpha.api :as t]
            [nl.epij.eledger.payee :as payee]
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
   'eledger/payee     (comp special-payees str-or-keyword)
   'eledger/account   (comp special-accounts str-or-keyword)
   'eledger/line-item (fn [line-item] (->> line-item
                                           (into {} (remove (comp str/blank? str second)))))})

(defn parse
  [ledger-output]
  (->> ledger-output
       (str/split-lines)
       (map (partial edn/read-string {:readers readers}))))

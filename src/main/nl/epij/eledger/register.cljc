(ns nl.epij.eledger.register
  (:require [clojure.tools.reader :as reader]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [tick.alpha.api :as t]
            [nl.epij.eledger.payee :as payee]
            [nl.epij.eledger.account :as account]
            [nl.epij.eledger.monetary-amount :as monetary-amount]
            [nl.epij.eledger.line-item :as line-item]
            [nl.epij.eledger :as eledger]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def special-payees {"Commodities revalued" ::payee/commodities-revalued
                     "<Unspecified payee>"  ::payee/unspecified-payee})

(def special-accounts {"<Revalued>"   ::account/revalued
                       "<Adjustment>" ::account/adjustment})

(defn str-or-keyword
  [x]
  (let [stream (ByteArrayInputStream. (.getBytes (pr-str x)))]
    (transit/read (transit/reader stream :json))))

(def readers
  {'time/date         t/parse
   'eledger/payee     (comp (fn [payee] (get special-payees payee payee)) str-or-keyword)
   'eledger/account   (comp (fn [account] (get special-accounts account account)) str-or-keyword)
   'eledger/line-item (fn [line-item]
                        (let [line-item (->> line-item
                                             (into {} (remove (comp str/blank? str second))))
                              {:keys [::line-item/commodity ::line-item/exchange]} line-item]
                          (-> line-item
                              (update ::line-item/amount
                                      (fn [amt]
                                        (if commodity
                                          {::monetary-amount/commodity commodity
                                           ::monetary-amount/value     (-> amt (str/replace commodity "") str/trim)}
                                          {::monetary-amount/value (str/trim amt)})))
                              (update ::line-item/exchange-amount
                                      (fn [amt]
                                        (if exchange
                                          {::monetary-amount/commodity exchange
                                           ::monetary-amount/value     (-> amt (str/replace exchange "") str/trim)}
                                          {::monetary-amount/value (str/trim amt)})))
                              (update ::line-item/exchange-total-amount
                                      (fn [amt]
                                        (if exchange
                                          {::monetary-amount/commodity exchange
                                           ::monetary-amount/value     (-> amt (str/replace exchange "") str/trim)}
                                          {::monetary-amount/value (str/trim amt)}))))))})

(defn parse
  [output-key]
  (fn [ledger-result]
    (let [{:keys [::eledger/output]} ledger-result
          line-items (when output (edn/read-string {:readers readers} (str "[" output "]")))]
      (assoc ledger-result output-key line-items))))

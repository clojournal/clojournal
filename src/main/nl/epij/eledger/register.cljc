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
                        (let [{:keys [::line-item/commodity ::line-item/exchange]} line-item
                              exchange  (when-not (str/blank? exchange) exchange)
                              commodity (when-not (str/blank? commodity) commodity)]
                          (into {}
                                (remove (fn [field] (or (-> field second str str/blank?)
                                                        (-> field second nil?))))
                                (-> line-item
                                    (update ::line-item/amount
                                            (fn [amt]
                                              (if commodity
                                                {::monetary-amount/commodity commodity
                                                 ::monetary-amount/value     (-> amt (str/replace commodity "") str/trim)}
                                                {::monetary-amount/value (str/trim amt)})))
                                    (update ::line-item/exchange-amount
                                            (fn [amt]
                                              (when exchange
                                                {::monetary-amount/commodity exchange
                                                 ::monetary-amount/value     (-> amt (str/replace exchange "") str/trim)})))
                                    (update ::line-item/exchange-total-amount
                                            (fn [amt]
                                              (when (and exchange (not (str/blank? amt)))
                                                {::monetary-amount/commodity exchange
                                                 ::monetary-amount/value     (-> amt (str/replace exchange "") str/trim)})))))))})

(defn parse
  [output-key]
  (fn [ledger-result]
    (let [{:keys [::eledger/output]} ledger-result
          line-items (when output (edn/read-string {:readers readers} (str "[" output "]")))]
      (assoc ledger-result output-key line-items))))

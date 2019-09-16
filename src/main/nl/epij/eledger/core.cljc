(ns nl.epij.eledger.core
  (:require [nl.epij.eledger :as eledger]
            [tick.alpha.api :as t]
            [clojure.spec.alpha :as s]
            [nl.epij.eledger.journal :as journal]
            [clojure.test.check.generators :as gen]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(s/def ::eledger/date (s/with-gen string?
                                  #(gen/fmap
                                     (fn [inst]
                                       (let [d (t/date inst)]
                                         (when (and (t/> d (t/date "1400-01-01"))
                                                    (t/< d (t/date "9999-01-01")))
                                           (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") inst))))
                                     (s/gen inst?))))

(s/def ::non-blank-string (s/and string? (comp not str/blank?)))

(s/def ::eledger/transaction-id (s/and (s/or :uuid uuid?
                                             :string ::non-blank-string
                                             :keyword keyword?)
                                       some?))

(s/def ::eledger/payee (s/or :keyword keyword?
                             :string ::non-blank-string))

(s/def ::acceptable-amount (s/and double? some? #(> % -1e230) #(< % 1e230)))

(s/def ::eledger/amount (s/with-gen (s/and string? some?)
                                    (fn [] (gen/fmap
                                             (fn [[amt decimals]] (str "EUR " (format (str "%." decimals "f") amt)))
                                             (gen/tuple (s/gen ::acceptable-amount)
                                                        (gen/large-integer* {:min 0 :max 10}))))))

(s/def ::eledger/account (s/and (s/or :keyword keyword?
                                      :string ::non-blank-string)
                                some?))

(s/def ::eledger/balance (s/with-gen (s/and string? some?)
                                     (fn [] (gen/fmap
                                              (fn [amt] (str "EUR " (format "%.0f" amt)))
                                              (s/gen ::acceptable-amount)))))

(s/def ::eledger/memo (s/and string? some? #(not (str/blank? %))))

(s/def ::eledger/posting (s/keys :req [::eledger/account]
                                 :opt [::eledger/balance
                                       ::eledger/amount
                                       ::eledger/memo]))

(s/def ::eledger/postings (s/with-gen (s/coll-of ::eledger/posting)
                                      #(gen/fmap
                                         (fn [postings]
                                           (let [[first-post & rem-post] postings]
                                             (conj rem-post (dissoc first-post ::eledger/amount ::eledger/balance))))
                                         (s/gen (s/or :no-postings (s/coll-of ::eledger/posting :count 0)
                                                      :postings (s/coll-of (s/keys :req [::eledger/account
                                                                                         ::eledger/amount]
                                                                                   :opt [#_::eledger/balance
                                                                                         ::eledger/memo])
                                                                           :min-count 2))))))

(s/def ::eledger/transaction (s/keys :req [::eledger/date]
                                     :opt [::eledger/transaction-id
                                           ::eledger/postings
                                           ::eledger/payee]))

(s/def ::eledger/transactions (s/coll-of ::eledger/transaction))

(s/def ::exit #(= 0 %))

(defn balance
  [txs]
  (let [sample (journal/anti-corrupt txs)
        result (sh/sh "ledger" "source" "-f" "-" "--permissive" :in sample)]
    (assoc result :journal sample)))

(s/fdef balance
        :args (s/cat :transactions ::eledger/transactions)
        :ret (s/keys :req-un [::exit]))

(comment

  (gen/sample (s/gen ::eledger/transaction))

  (balance (gen/sample (s/gen ::eledger/transaction) 100))

  (s/exercise-fn `balance 1)

  )

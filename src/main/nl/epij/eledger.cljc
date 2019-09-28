(ns nl.epij.eledger
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [tick.alpha.api :as t]
            [clojure.string :as str]
            [nl.epij.eledger.monetary-amount :as monetary-amount]
            [nl.epij.eledger.register :as register]))


(s/def ::date (s/or :date (s/with-gen #(= (type %) java.time.LocalDate)
                                      #(gen/fmap
                                         (fn [inst]
                                           (let [d (t/date inst)]
                                             (when (and (t/> d (t/date "1400-01-01"))
                                                        (t/< d (t/date "9999-01-01")))
                                               d)))
                                         (s/gen inst?)))
                    :string (s/with-gen string?
                                        #(gen/fmap
                                           (fn [inst]
                                             (let [d (t/date inst)]
                                               (when (and (t/> d (t/date "1400-01-01"))
                                                          (t/< d (t/date "9999-01-01")))
                                                 (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") inst))))
                                           (s/gen inst?)))))

(s/def ::non-blank-string (s/and string? (comp not str/blank?)))

(s/def ::non-start-number (partial re-matches #"^\D.+"))

(s/def ::transaction-id (s/and (s/or :uuid uuid?
                                     :string ::non-blank-string
                                     :keyword keyword?)
                               some?))

(s/def ::payee (s/or :keyword keyword?
                     :string (s/and ::non-blank-string ::non-start-number)))

(s/def ::acceptable-amount (s/double-in :infinite? false :NaN? false :min 1e-240 :max 1e240))

(s/def ::amount (s/with-gen (s/and string? some?)
                            (fn [] (gen/fmap
                                     (fn [[amt decimals]] (str "R$ " (format (str "%." decimals "f") amt)))
                                     (gen/tuple (s/gen ::acceptable-amount)
                                                (gen/large-integer* {:min 0 :max 10}))))))

(s/def ::account (s/and (s/or :keyword keyword?
                              :string (s/and ::non-blank-string ::non-start-number))
                        some?))

(s/def ::balance (s/with-gen (s/and string? some?)
                             (fn [] (gen/fmap
                                      (fn [amt] (str "EUR " (format "%.0f" amt)))
                                      (s/gen ::acceptable-amount)))))

(s/def ::memo (s/and string? some? #(not (str/blank? %))))

(s/def ::posting (s/keys :req [::account]
                         :opt [::balance
                               ::amount
                               ::memo]))

(s/def ::postings (s/with-gen (s/coll-of ::posting :gen-max 2)
                              #(gen/fmap
                                 (fn [postings]
                                   (let [[first-post & rem-post] postings]
                                     (conj rem-post (dissoc first-post ::amount ::balance))))
                                 (s/gen (s/or :no-postings (s/coll-of ::posting :count 0)
                                              :postings (s/coll-of (s/keys :req [::account
                                                                                 ::amount]
                                                                           :opt [#_::balance
                                                                                 ::memo])
                                                                   :min-count 2))))))

(s/def ::transaction (s/keys :req [::date]
                             :opt [::transaction-id
                                   ::postings
                                   ::payee]))

(s/def ::transactions (s/coll-of ::transaction :gen-max 20))

(s/def ::exit #(= 0 %))

(s/def ::commodity string?)
(s/def ::exchange ::commodity)

(s/def ::monetary-amount/commodity ::commodity)
(s/def ::monetary-amount/value string?)

(s/def ::monetary-amount (s/keys :req [::monetary-amount/value]
                                 :opt [::monetary-amount/commodity]))

(s/def ::register/date ::date)
(s/def ::register/account ::account)
(s/def ::register/payee ::payee)
(s/def ::register/commodity ::commodity)
(s/def ::register/exchange ::exchange)
(s/def ::register/amount ::monetary-amount)
(s/def ::register/exchange-amount ::monetary-amount)
(s/def ::register/exchange-total-amount ::monetary-amount)
(s/def ::register/memo ::memo)
(s/def ::register/transaction-id ::transaction-id)

(s/def ::line-item (s/keys :req [::register/date
                                 ::register/account
                                 ::register/payee

                                 ::register/commodity
                                 ::register/amount

                                 ::register/exchange-amount
                                 ::register/exchange-total-amount]
                           :opt [::register/transaction-id
                                 ::register/memo
                                 ::register/exchange]))

(s/def ::line-items (s/coll-of ::line-item))

(comment

  (s/exercise ::line-item)

  )

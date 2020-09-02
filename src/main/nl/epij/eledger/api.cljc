(ns nl.epij.eledger.api
  (:require [nl.epij.eledger.journal :as journal]
            [nl.epij.eledger :as eledger]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [nl.epij.eledger.line-item :as line-item]
            [clojure.edn :as edn]))

(defn journal
  "Takes a coll of transactions and returns a ledger journal"
  ([transactions] (journal transactions {}))
  ([transactions options]
   (journal/anti-corrupt transactions options)))

(def default-output-fields
  "Default EDN output fields"
  {::line-item/date                  "#time/date %(quoted(format_date(date)))"
   ::line-item/transaction-id        "%(quoted(code))"
   ::line-item/payee                 "#eledger/payee %(quoted(payee))"
   ::line-item/account               "#eledger/account %(quoted(display_account))"
   ::line-item/memo                  "%(quoted(note))"
   ::line-item/transaction-memo      "%(quoted(xact.note))"

   ::line-item/commodity             "%(quoted(commodity))"
   ::line-item/amount                "\"%(post.commodity) %(quantity(parent.amount))\""

   ::line-item/exchange              "%(quoted(exchange))"
   ::line-item/exchange-amount       "\"%(commodity(display_amount)) %(quantity(scrub(display_amount)))\""
   ::line-item/exchange-total-amount "%(quoted(display_total))"})

(defn csv-format
  ([output-fields]
   (csv-format output-fields nil))
  ([output-fields reader-tag]
   (format "%s{%s}\n"
           (or (some-> reader-tag (str " ")) "")
           (str/join " " (into [] cat output-fields)))))

(def readers {'eledger/lot (fn [x]
                             (mapv (fn [y]
                                     (let [[_ quantity price date] (re-matches #"(.+) \{(.+)\} \[(.+)\]" y)]
                                       {:nl.epij.eledger.lot/quantity quantity
                                        :nl.epij.eledger.lot/price    price
                                        :nl.epij.eledger.lot/date     date}))
                                   (str/split-lines x)))})

(defn eledger
  "Takes a journal and vanilla Ledger CLI arguments passed through clojure.java.shell/sh"
  [journal args]
  (let [journal-data (cond (string? journal) journal
                           :else (slurp journal))
        args'        (concat ["--file" "-"] (flatten args))
        ledger-args  (concat ["ledger"]
                             (map (fn [x]
                                    (cond
                                      (string? x) x
                                      (map? x) (csv-format x)))
                                  args')
                             [:in journal-data])
        sh-result    (apply shell/sh ledger-args)
        f            (comp (partial edn/read-string {:readers readers}) #(format "[%s]" %))]
    (case (get sh-result :exit)
      0 {:nl.epij.eledger.report/output (f (get sh-result :out))}
      {:nl.epij.eledger.report/error (get sh-result :err)})))

;(s/fdef eledger
;        :args (s/cat :transactions ::eledger/transactions
;                     :command (s/with-gen keyword? #(gen/return ::eledger/edn-register))
;                     :query (s/with-gen (s/coll-of string?) #(gen/return []))
;                     :options (s/with-gen map? #(gen/return {})))
;        :ret (s/keys :req [::eledger/line-items]))

(comment

  (set! *print-length* 20)

  (::eledger/line-items (eledger [{::eledger/date           "2019-06-01"
                                   ::eledger/transaction-id #uuid "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"
                                   ::eledger/payee          "Mister Shawarma"
                                   ::eledger/postings       [{::eledger/account :expenses/food
                                                              ::eledger/amount  "R$ 20"}
                                                             {::eledger/account :assets/cash}]}
                                  {::eledger/date     #time/date "2019-07-01"
                                   ::eledger/payee    "Interactive Brokers"
                                   ::eledger/postings [{::eledger/account :assets/stocks
                                                        ::eledger/amount  "USD 1336"}
                                                       {::eledger/account :expenses/commissions
                                                        ::eledger/amount  "USD 1"}
                                                       {::eledger/account :assets/checking}]}]
                                 ::eledger/edn-register))

  )

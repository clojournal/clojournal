(ns nl.epij.eledger.api
  (:require [nl.epij.eledger.journal :as journal]
            [nl.epij.eledger :as eledger]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [nl.epij.eledger.register :as register]
            [nl.epij.eledger.line-item :as line-item]))

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

(defn eledger
  "Takes a coll of transactions, a command, and (optionally) options

  command can either be:
  - The keyword ::eledger/edn-register which outputs an EDN register
  - Any string which is passed to ledger CLI verbatim (like register, balance, bal, etc.)"
  ([transactions command query]
   (eledger transactions command query {}))
  ([transactions command query options]
   (let [{:keys [::eledger/ledger-options ::eledger/output-fields]} options
         ledger-options    (or ledger-options [])
         output-fields     (or output-fields default-output-fields)
         journal           (journal transactions (select-keys options [::eledger/prices]))
         csv-format        (str "#eledger/line-item {" (str/join " " (into [] cat output-fields)) "}\n")
         command           (case command
                             ::eledger/edn-register {::eledger/command           command
                                                     ::eledger/ledger-command    "csv"
                                                     ::eledger/env               {"LEDGER_CSV_FORMAT"  csv-format
                                                                                  "LEDGER_DATE_FORMAT" "%Y-%m-%d"}
                                                     ::eledger/transformation-fn (register/parse ::eledger/line-items)}
                             {::eledger/command           ::eledger/ledger-command
                              ::eledger/ledger-command    command
                              ::eledger/transformation-fn identity})
         ledger-env        (merge {"LEDGER_FILE" "-"} (get command ::eledger/env))
         ledger-args       (concat ["ledger"]
                                   (mapcat #(vector (str "--" (name (first %))) (second %)) ledger-options)
                                   [(get command ::eledger/ledger-command)]
                                   query
                                   [:in journal
                                    :env ledger-env])
         sh-result         (apply shell/sh ledger-args)
         output            (case (get sh-result :exit)
                             0 {::eledger/output (get sh-result :out)}
                             {::eledger/error (get sh-result :err)})
         transformation-fn (get command ::eledger/transformation-fn)]
     (transformation-fn output))))

(s/fdef eledger
        :args (s/cat :transactions ::eledger/transactions
                     :command (s/with-gen keyword? #(gen/return ::eledger/edn-register))
                     :query (s/with-gen (s/coll-of string?) #(gen/return []))
                     :options (s/with-gen map? #(gen/return {})))
        :ret (s/keys :req [::eledger/line-items]))

(comment

  (set! *print-length* 20)

  (gen/sample (s/gen ::eledger/transactions) 1)

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

  (s/exercise-fn `eledger 1)

  (eledger (gen/sample (s/gen ::eledger/transaction)) "bal")



  (journal (gen/sample (s/gen ::eledger/transaction)))

  (gen/sample (s/gen ::eledger/transaction))

  )

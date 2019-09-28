(ns nl.epij.eledger.api
  (:require [nl.epij.eledger.journal :as journal]
            [nl.epij.eledger :as eledger]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [nl.epij.eledger.register :as register]))

(defn journal
  "Takes a coll of transactions and returns a ledger journal"
  [transactions]
  (journal/anti-corrupt transactions))

(def default-output-fields
  "Default EDN output fields"
  {::eledger/date                  "#time/date %(quoted(format_date(date)))"
   ::eledger/transaction-id        "%(quoted(code))"
   ::eledger/payee                 "#eledger/payee %(quoted(payee))"
   ::eledger/account               "#eledger/account %(quoted(display_account))"
   ::eledger/memo                  "%(quoted(note))"
   ::eledger/transaction-memo      "%(quoted(xact.note))"

   ::eledger/commodity             "%(quoted(commodity))"
   ::eledger/amount                "\"%(post.commodity) %(quantity(parent.amount))\""

   ::eledger/exchange              "%(quoted(exchange))"
   ::eledger/exchange-amount       "\"%(commodity(display_amount)) %(quantity(scrub(display_amount)))\""
   ::eledger/exchange-total-amount "%(quoted(display_total))"})

(defn eledger
  "Takes a coll of transactions and ledger CLI arguments and returns its output (verbatim or EDN)"
  ([transactions command]
   (eledger transactions command {}))
  ([transactions command options]
   (let [{:keys [::eledger/ledger-options ::eledger/output-fields]} options
         ledger-options (or ledger-options [])
         output-fields  (or output-fields default-output-fields)
         journal        (journal transactions)
         csv-format     (str "#eledger/line-item {" (str/join " " (into [] cat output-fields)) "}\n")
         command        (case command
                          ::eledger/edn-register {::eledger/command  command
                                                  ::eledger/parse-fn register/parse}
                          {::eledger/command        ::eledger/ledger-command
                           ::eledger/ledger-command command
                           ::eledger/parse-fn       identity})
         cmd-result     (apply shell/sh
                               (case (get command ::eledger/command)
                                 ::eledger/edn-register (concat ["ledger"]
                                                                (mapcat #(vector (str "--" (name (first %))) (second %)) ledger-options)
                                                                ["csv"
                                                                 :in journal
                                                                 :env {"LEDGER_FILE"        "-"
                                                                       "LEDGER_CSV_FORMAT"  csv-format
                                                                       "LEDGER_DATE_FORMAT" "%Y-%m-%d"}])
                                 ::eledger/ledger-command (concat ["ledger"]
                                                                  (mapcat #(vector (str "--" (name (first %))) (second %)) ledger-options)
                                                                  [(get command ::eledger/ledger-command)
                                                                   :in journal
                                                                   :env {"LEDGER_FILE" "-"}])))
         output         (case (get cmd-result :exit)
                          0 {::eledger/output (get cmd-result :out)}
                          {::eledger/error (get cmd-result :err)})]
     ((get command ::eledger/parse-fn) (some-> output ::eledger/output)))))


(comment

  (eledger (gen/sample (s/gen ::eledger/transaction))
           "bal"
           {::eledger/ledger-options {:price-db "/tmp/prices.txt"
                                      :exchange "€"}})

  (eledger (gen/sample (s/gen ::eledger/transaction))
           ::eledger/edn-register
           {::eledger/ledger-options {:price-db "/tmp/prices.txt"
                                      :exchange "€"}})

  (journal (gen/sample (s/gen ::eledger/transaction)))

  (gen/sample (s/gen ::eledger/transaction))

  )

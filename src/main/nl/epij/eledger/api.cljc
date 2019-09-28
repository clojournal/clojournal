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

(def output-fields
  "Default EDN output fields"
  {::eledger/date                  "#time/date %(quoted(format_date(date)))"
   ::eledger/transaction-id        "%(quoted(code))"
   ::eledger/payee                 "#eledger/payee %(quoted(payee))"
   ::eledger/account               "#eledger/account %(quoted(display_account))"
   ::eledger/memo                  "%(quoted(note))"
   ::eledger/transaction-memo      "%(quoted(xact.note))"
   ::eledger/local-currency        "%(quoted(post.commodity))"
   ::eledger/local-currency-amount "%(quoted(quantity(parent.amount)))"
   ::eledger/base-amount           "#eledger/amount \"%(commodity(display_amount)) %(quantity(scrub(display_amount)))\""
   ::eledger/base-total-amount     "#eledger/amount %(quoted(display_total))"})

(defn eledger
  "Takes a coll of transactions and ledger CLI arguments and returns its output (verbatim or EDN)"
  ([transactions command]
   (eledger transactions command []))
  ([transactions command ledger-args]
   (let [journal    (journal transactions)
         csv-format (str "#eledger/line-item {" (str/join " " (into [] cat output-fields)) "}
")
         cmd-result (apply shell/sh (concat ["ledger"]
                                            [command]
                                            ledger-args
                                            [:in journal
                                             :env {"LEDGER_FILE"        "-"
                                                   "LEDGER_CSV_FORMAT"  csv-format
                                                   "LEDGER_DATE_FORMAT" "%Y-%m-%d"}]))
         output     (case (get cmd-result :exit)
                      0 {::eledger/output (get cmd-result :out)}
                      {::eledger/error (get cmd-result :err)})]
     (println output)
     (some-> output ::eledger/output register/parse))))


(comment

  (eledger (gen/sample (s/gen ::eledger/transaction)) "csv")

  (journal (gen/sample (s/gen ::eledger/transaction)))

  (gen/sample (s/gen ::eledger/transaction))

  )

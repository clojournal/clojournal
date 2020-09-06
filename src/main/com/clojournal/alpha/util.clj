(ns com.clojournal.alpha.util
  (:require [clojure.string :as str])
  (:import (java.util Base64)))

(defn print-debug
  [report]
  (let [{journal :com.clojournal.alpha.report/journal
         args    :com.clojournal.alpha.report/args} report
        debug-string (format "echo %s | base64 --decode | ledger %s"
                             (String. (.encode (Base64/getEncoder) (.getBytes journal)))
                             (str/join " " (map (fn [x]
                                                  (cond
                                                    (string? x)
                                                    x

                                                    (map? x)
                                                    (format "'{%s}\n'" (str/join " " (into [] cat x)))))
                                                args)))]
    (println (str/escape debug-string char-escape-string))))

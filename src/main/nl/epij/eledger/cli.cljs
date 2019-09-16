(ns nl.epij.eledger.cli
  (:require [cljs.nodejs.shell :as shell]
            [nl.epij.eledger.journal :as journal]
            [cljs.reader :as reader]
            [tick.alpha.api :as t]
            [time-literals.data-readers]))

(def stdinput (atom ""))

(defn -main [args]
  ;; https://gist.github.com/borkdude/07b97a3d7fb9183598b7b2a940e35407
  (js/require "process")

  (.setEncoding js/process.stdin "utf8")
  (.on js/process.stdin "data"
       (fn [data]
         (swap! stdinput #(str % data))))

  (.on js/process.stdin "end"
       (fn []
         (swap! stdinput (fn [s]
                           (subs s 0 (dec (count s)))))

         (println (journal/anti-corrupt (reader/read-string {:readers {'time/date t/parse}} @stdinput))))))

(ns sql-to-spec.parsers.ddl
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [instaparse.core :refer [defparser transform]]))

(defparser whitespace
  "whitespace = #'\\s+'")

(defparser parser
  (io/resource "ddl.grammar")
  :string-ci true
  :auto-whitespace whitespace)

(defn parse [s]
  (->> s
       parser
       (transform {:number edn/read-string
                   :DATETIME (comp vector keyword)})))

(ns sql-to-spec.parsers.ddl
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [instaparse.core :refer [defparser transform]]
            [clojure.spec :as s]))

(defparser whitespace
  "whitespace = #'\\s+'")

(defparser parser
  (io/resource "ddl.grammar")
  :string-ci true
  :auto-whitespace whitespace)

(defn parse [sql]
  (->> sql
       parser
       (transform {:number edn/read-string
                   :DATETIME (comp vector keyword)})))

(defmulti data-type->spec (fn [[tag & specs]] tag))

(defmethod data-type->spec :INTEGER [_]
  (s/spec int?))

(defmethod data-type->spec :VARCHAR [[_ limit]]
  (s/spec (s/and string?
                 #(<= (.length %) limit))))

(defmulti op (fn [[op & tail]] op))

(defn add-coldefs [m table-name specs]
  (let [{:keys [column_name data_type]} (into {} specs)]
    (assoc m (keyword table-name column_name) (data-type->spec data_type))))

(defn coldefs->map [table-name coldefs]
  (reduce (fn [acc [_ & specs]]
            (add-coldefs acc table-name specs))
          {}
          coldefs))

(defmethod op :CREATE [[_ table-name & coldefs]]
  (coldefs->map table-name coldefs))

(defmulti alter-op (fn [table-name [_ [op & coldefs]]] op))

(defmethod alter-op :add [table-name [_ & coldefs]]
  (coldefs->map table-name coldefs))

(defmethod op :ALTER [[_ table-name & alter-ops]]
  (first (map #(alter-op table-name %) alter-ops)))

(defn table [sql]
  (->> sql
       parse
       rest ; Skips :S, start of grammar
       (map op)
       (apply merge)))

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

(defmethod data-type->spec :DOUBLE-PRECISION [_]
  (s/spec #(instance? Double %)))

(defmethod data-type->spec :REAL [_]
  (s/spec #(instance? Float %)))

(defmethod data-type->spec :TEXT [_]
  (s/spec (s/and string?
                 #(<= (.length %) 256))))

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

(defmethod alter-op :drop [table-name [_ [_ [_ column-name]]]]
  {(keyword table-name column-name) nil})

(defmethod alter-op :rename-table [old-name [_ [_ [_ new-name]]]]
  {:rename-namespace {:old-name old-name
                      :new-name new-name}})

(defmethod op :ALTER [[_ table-name & alter-ops]]
  (first (map #(alter-op table-name %) alter-ops)))

(defmethod op :INSERT [_])
(defmethod op :DELETE [_])
(defmethod op :DROP [_])
(defmethod op :comment [_])

(defn rename-namespace [rename-map m]
  (let [{{:keys [old-name new-name]} :rename-namespace} rename-map]
    (into {} (for [[k v :as entry] m]
               (if (= (namespace k) old-name)
                 [(keyword new-name (name k)) v]
                 entry)))))

(defn table [sql]
  (->> sql
       parse
       rest ; Skips :S, start of grammar
       (map op)
       (reduce (fn [acc v]
                 (if (:rename-namespace v)
                   (rename-namespace v acc)
                   (merge acc v)))
               {})
       (filter (comp not nil? val))
       (into {})))

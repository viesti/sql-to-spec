(ns sql-to-spec.parsers.ddl-test
  (:require [sql-to-spec.parsers.ddl :as sut]
            [clojure.test :refer :all]))

(deftest parser
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "foo"]
          [:coldef
           [:column_name "id"]
           [:data_type [:INTEGER]]]]
         (sut/parse "create table foo (id int);"))
      "Simple table")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "foo"]
          [:coldef
           [:column_name "name"]
           [:data_type [:VARCHAR 200]]]]
         (sut/parse "create table foo (name varchar(200));"))
      "varchar column")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "moi"]
          [:coldef
           [:column_name "id"]
           [:data_type [:INTEGER]]]]
         (sut/parse "create table if not exists moi (id int);"))
      "If not exists")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "two_columns"]
          [:coldef
           [:column_name "id"]
           [:data_type [:INTEGER]]]
          [:coldef
           [:column_name "name"]
           [:data_type [:TEXT]]]]
         (sut/parse "create table two_columns (id int, name text);"))
      "Two columns")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "test"]
          [:coldef
           [:column_name "id"]
           [:data_type [:INTEGER]]
           [:column_attributes "distkey" "sortkey"]]]
         (sut/parse "create table test (id int distkey sortkey);"))
      "Column attributes")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "test"]
          [:coldef
           [:column_name "id"]
           [:data_type [:INTEGER]]
           [:column_attributes "distkey"]]
          [:coldef
           [:column_name "ts"]
           [:data_type [:timestamp]]
           [:column_attributes "sortkey"]]]
         (sut/parse "create table test (id int distkey, ts timestamp sortkey);"))
      "Column attributes on two columns")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "test"]
          [:coldef
           [:column_name "id"]
           [:data_type [:INTEGER]]
           [:column_attributes "distkey"]
           [:column_constraints "not null"]]]
         (sut/parse "create table test (id int distkey not null);"))
      "Columnt constraints")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "test"]
          [:coldef
           [:column_name "id"]
           [:data_type [:INTEGER]]]
          [:coldef
           [:column_name "ts"]
           [:data_type [:timestamp]]]
          [:table_attribute
           [:distkey [:column_name "id"]]
           [:sortkey [:column_name "ts"] [:column_name "id"]]]]
         (sut/parse "create table test (id int, ts timestamp) distkey(id) sortkey(ts, id);")))
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "test"]
          [:coldef
           [:column_name "price"]
           [:data_type [:DECIMAL [:precision 9] [:scale 2]]]]]
         (sut/parse "create table test (price decimal(9,2))"))
      "decimal")
  (is (= [:S
          [:CREATE "CREATE"]
          [:TABLE "TABLE"]
          [:table_name "test"]
          [:coldef
           [:column_name "a"]
           [:data_type [:BIGINT]]]]
         (sut/parse "create table test (a int8)"))))

(ns sql-to-spec.parsers.ddl-test
  (:require [sql-to-spec.parsers.ddl :as sut]
            [clojure.test :refer :all]
            [clojure.spec :as s]))

(deftest parse
  (is (= [:S
          [:CREATE "foo"
           [:coldef
            [:column_name "id"]
            [:data_type [:INTEGER]]]]]
         (sut/parse "create table foo (id int);"))
      "Simple table")
  (is (= [:S
          [:CREATE "foo"
           [:coldef
            [:column_name "name"]
            [:data_type [:VARCHAR 200]]]]]
         (sut/parse "create table foo (name varchar(200));"))
      "varchar column")
  (is (= [:S
          [:CREATE "moi"
           [:coldef
            [:column_name "id"]
            [:data_type [:INTEGER]]]]]
         (sut/parse "create table if not exists moi (id int);"))
      "If not exists")
  (is (= [:S
          [:CREATE "two_columns"
           [:coldef
            [:column_name "id"]
            [:data_type [:INTEGER]]]
           [:coldef
            [:column_name "name"]
            [:data_type [:TEXT]]]]]
         (sut/parse "create table two_columns (id int, name text);"))
      "Two columns")
  (is (= [:S
          [:CREATE "test"
           [:coldef
            [:column_name "id"]
            [:data_type [:INTEGER]]
            [:column_attributes "distkey" "sortkey"]]]]
         (sut/parse "create table test (id int distkey sortkey);"))
      "Column attributes")
  (is (= [:S
          [:CREATE "test"
           [:coldef
            [:column_name "id"]
            [:data_type [:INTEGER]]
            [:column_attributes "distkey"]]
           [:coldef
            [:column_name "ts"]
            [:data_type [:timestamp]]
            [:column_attributes "sortkey"]]]]
         (sut/parse "create table test (id int distkey, ts timestamp sortkey);"))
      "Column attributes on two columns")
  (is (= [:S
          [:CREATE "test"
           [:coldef
            [:column_name "id"]
            [:data_type [:INTEGER]]
            [:column_attributes "distkey"]
            [:column_constraints "not null"]]]]
         (sut/parse "create table test (id int distkey not null);"))
      "Columnt constraints")
  (is (= [:S
          [:CREATE "test"
           [:coldef
            [:column_name "id"]
            [:data_type [:INTEGER]]]
           [:coldef
            [:column_name "ts"]
            [:data_type [:timestamp]]]
           [:table_attribute
            [:distkey [:column_name "id"]]
            [:sortkey [:column_name "ts"] [:column_name "id"]]]]]
         (sut/parse "create table test (id int, ts timestamp) distkey(id) sortkey(ts, id);")))
  (is (= [:S
          [:CREATE "test"
           [:coldef
            [:column_name "price"]
            [:data_type [:DECIMAL [:precision 9] [:scale 2]]]]]]
         (sut/parse "create table test (price decimal(9,2))"))
      "decimal")
  (is (= [:S
          [:CREATE "test"
           [:coldef
            [:column_name "a"]
            [:data_type [:BIGINT]]]]]
         (sut/parse "create table test (a int8)"))))

(deftest alter-table
  (is (= [:S
          [:ALTER "test"
           [:alter-op
            [:add
             [:column_name "id"]
             [:data_type [:INTEGER]]]]]]
         (sut/parse "alter table test add column id int"))))

(deftest multiple-statements
  (is (= [:S
          [:CREATE "foo"
           [:coldef
            [:column_name "a"]
            [:data_type [:BIGINT]]]]
          [:ALTER "foo"
           [:alter-op
            [:add
             [:column_name "name"]
             [:data_type [:TEXT]]]]]]
         (sut/parse "
create table foo (a int8);
alter table foo add column name text"))))

(defn map-value [f coll]
  (into {} (for [[k v] coll] [k (f v)])))

(deftest table
  (is (= {:people/id (s/form (s/spec int?))}
         (map-value s/form (sut/table "create table people (id int)")))
      "simple create")
  (let [table (sut/table "create table people (id int);
                          alter table people add column name varchar(200);")]
    (is (= {:people/id (s/form (s/spec int?))
            :people/name (let [limit 200]
                           (s/form (s/spec (s/and string?
                                                  #(<= (.length %) limit)))))}
           (map-value s/form table))
        "create and alter with add column")
    (is (s/valid? (:people/name table)
                  (String. (byte-array 200) "UTF-8")))
    (is (not (s/valid? (:people/name table)
                       (String. (byte-array 201) "UTF-8")))))
  (let [table (sut/table "create table people (id int, name text);
                          alter table people drop column name;")]
    (is (= {:people/id (s/form (s/spec int?))}
           (map-value s/form table))))
  (testing "Rename table"
    (let [table (sut/table "create table foo (id int); alter table foo rename to bar;")]
      (is (= {:bar/id (s/form (s/spec int?))}
             (map-value s/form table))))))

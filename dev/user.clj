(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh clear]]
            [eftest.runner :refer [find-tests run-tests]]
            [clojure.repl :refer :all]
            [instaparse.core :as insta]
            [clojure.spec :as s]))

(defn do-test []
  (run-tests (find-tests "test") {:report clojure.test/report}))

(defn reset []
  (clear)
  (refresh :after 'user/do-test))

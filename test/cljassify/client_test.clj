(ns cljassify.client-test
  (:require [clojure.test :refer :all]
            [cljassify.client :refer :all]
            [clj-http.fake :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (with-fake-routes
      {"http://localhost:8080/info" (fn [req] {:status 200 :body "true"})}
      (println (info)))
    (is (= 0 1))))

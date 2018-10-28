(ns cljassify.client-test
  (:require [clojure.test :refer :all]
            [cljassify.client :refer :all]
            [clj-http.fake :refer :all]
            [clojure.java.io :as io]
            [cheshire.core :refer :all]))

(deftest model-test
  (testing "model definition"
    (let [classes '("class1" "class2")
          h (hash classes)]
      (is (= (:name (model classes)) (str h "-n")))
      (is (= (:id (model classes)) (str h "-i")))
      (is (= classes (:classes (model classes))))
      (is (= {:ngrams 1} (:options (model classes :ngrams 1))))
      (is (= {:skipgrams 2} (:options (model classes :skipgrams 2))))
      (is (= {:ngrams 1 :skipgrams 2} (:options (model classes :ngrams 1 :skipgrams 2))))
      (is (= "modelName" (:name (model classes :name "modelName"))))
      (is (= "modelId" (:id (model classes :id "modelId"))))
      (is (thrown? IllegalArgumentException (model "foo")))
      (is (thrown? IllegalArgumentException (model []))))))

(deftest box-test
  (testing "box definition"
    (is (= {:host "localhost"
            :port 8080
            :username nil
            :password nil
            :debug false
            :body-only true}
           *default-box*))
    (is (= {:host "otherhost"
            :port 8081
            :username nil
            :password nil
            :debug false
            :body-only true}
           (box "otherhost" 8081)))
    (is (= {:host "localhost"
            :port 8082
            :username "robin"
            :password "banks"
            :debug true
            :body-only false}
           (box "localhost" 8082 :username "robin" :password "banks" :debug true :body-only false)))))

(defn slurp-file
  [x]
  (-> (str "responses/" x) io/resource slurp))

(deftest client-test
  (testing "client functions"
    (with-fake-routes {"http://localhost:8080/info"
                       (fn [r] {:status 200
                                :body (-> "responses/info.json" io/resource slurp)})}
      (is (= "ready" (:status (info)))))))

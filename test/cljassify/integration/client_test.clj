(ns cljassify.integration.client-test
  (:require [clojure.test :refer :all]
            [cljassify.client :refer :all]))

(def model-id "myModelId")

(defn model-fixture
  [f]
  (let [my-model (model ["class1" "class2"] :id model-id)]
    (do
      (let [response (create-model my-model)]
        (is (= ["class1" "class2"] (:classes response)))
        (is (= true (:success response)))
        (is (= {} (:options response)))))
    (f)
    (do
      (delete-model model-id))))

(def example {:class "class1"
              :inputs [{:key "user_age"
                        :type "number"
                        :value "25"}
                       {:key "user_interests"
                        :type "list"
                        :value "music,cooking,ml"}
                       {:key "user_location"
                        :type "keyword"
                        :value "London"}]})

(def examples {:examples
               [{:class "class1"
                 :inputs [{:key "user_age"
                           :type "number"
                           :value "25"}]}
                {:class "class2"
                 :inputs [{:key "user_age"
                           :type "number"
                           :value "55"}]}]})
(def features {:limit 10
               :inputs [{:key "user_age"
                         :type "number"
                         :value "25"}
                        {:key "user_interests"
                         :type "list"
                         :value "music,cooking,ml"}
                        {:key "user_location"
                         :type "keyword"
                         :value "London"}]})

(use-fixtures :each model-fixture)

(deftest ^:integration info-test
  (testing "info endpoint"
    (let [response (info)]
      (is (= true (:success response)))
      (is (= "classificationbox" (:name response))))))

(deftest ^:integration healthz-test
  (testing "healthz endpoint"
    (let [response (healthz)]
      (is (= true (:success response)))
      (is (= "classificationbox" (-> response :metadata :boxname))))))

(deftest ^:integration readyz-test
  (testing "readyz endpoint"
    (let [response (readyz)]
      (is (= nil response)))))

(deftest ^:integration liveness-test
  (testing "liveness endpoint"
    (let [response (liveness)]
      (is (= "OK" response)))))

(deftest ^:integration teach-model-test
  (testing "teach endpoint"
    (dotimes [n 5]
      (is (= {:success true} (teach-model model-id example))))))

(deftest ^:integration teach-model-multi-test
  (testing "teach multi endpoint"
    (dotimes [n 5]
      (is (= {:success true} (teach-model-multi model-id examples))))))

(deftest ^:integration predict-test
  (testing "predict endpoint"
    (dotimes [n 5]
      (is (= {:success true} (teach-model model-id example))))
    (let [response (predict model-id features)]
      (is (= true (:success response)))
      (is (= ["class1" "class2"] (map :id (-> response :classes))))
      (is (= '(true true) (keep #(and (> 1 %)
                                      (< 0 %))
                                (map :score (-> response :classes))))))))

(deftest ^:integration model-statistics-test
  (testing "model statistics endpoint"
    (let [response (model-statistics model-id)]
      (is (= {:success true, :predictions 0, :examples 0, :classes []}
             (model-statistics model-id)))
      (dotimes [n 5]
        (is (= {:success true} (teach-model model-id example))))
      (is (= {:success true, :predictions 0, :examples 5, :classes [{:name "class1", :examples 5}]}
             (model-statistics model-id))))))

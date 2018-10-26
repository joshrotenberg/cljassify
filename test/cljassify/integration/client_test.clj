(ns cljassify.integration.client-test
  (:require [clojure.test :refer :all]
            [cljassify.client :refer :all]))

(def model-id "myModelId")

(defn model-fixture
  [f]
  (let [my-model (model ["class1" "class2"] :id model-id)]
    (do
      (println "creating model")
      (create-model my-model))
    (f)
    (do
      (println "deleting model")
      (delete-model model-id))))

(use-fixtures :each model-fixture)

(deftest ^:integration info-test
  (testing "testing info endpoint"
    (println (info))))

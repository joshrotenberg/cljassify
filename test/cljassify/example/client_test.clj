(ns cljassify.example.client-test
  (:require [clojure.test :refer :all]
            [cljassify.client :refer :all]))

(def model-id "myModelId")

(defn model-fixture
  [f]
  (let [my-model (model ["class1" "class2"] :id model-id)]
    (create-model my-model)
    (f)
    (delete-model model-id)))

(use-fixtures :each model-fixture)

(deftest ^:example get-model-example
  (testing "get a model from the box"
    (println (get-model model-id))))

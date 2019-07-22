(ns cljassify.integration.client-test
  (:require [clojure.test :refer :all]
            [cljassify.client :refer :all]
            [clojure.java.io :as io])
  (:import org.testcontainers.containers.GenericContainer
           [org.testcontainers.containers.wait
            Wait WaitStrategy]))

(def test-box (atom nil))
(def model-id "myModelId")

(defn container-fixture
  [f]
  (let [c (doto (GenericContainer. "machinebox/classificationbox:latest")
            (.waitingFor (Wait/forHttp "/liveness"))
            (.withEnv "MB_KEY" (System/getenv "MB_KEY"))
            (.withEnv "MB_PORT" (get (System/getenv) "MB_PORT" "8080"))
            (.withExposedPorts (into-array Integer [(int 8080)])))]
    (do
      (.start c)
      (reset! test-box {:host (.getContainerIpAddress c)
                        :port (.getMappedPort c 8080)}))
    (f)
    (.stop c)))


(defn model-fixture
  [f]
  (let [my-model (model ["class1" "class2"] :id model-id)]
    (do
      (let [response (create-model @test-box my-model)]
        (is (= ["class1" "class2"] (:classes response)))
        (is (= true (:success response)))
        (is (= {} (:options response)))))
    (f)
    (is (= true (:success (delete-model @test-box model-id))))))

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


(use-fixtures :once container-fixture model-fixture)

(deftest ^:integration info-test
  (testing "info endpoint"
    (let [response (info @test-box)]
      (is (= true (:success response)))
      (is (= "classificationbox" (:name response))))))

(deftest ^:integration healthz-test
  (testing "healthz endpoint"
    (let [response (healthz @test-box)]
      (is (= true (:success response)))
      (is (= "classificationbox" (-> response :metadata :boxname))))))

(deftest ^:integration readyz-test
  (testing "readyz endpoint"
    (let [response (readyz @test-box)]
      (is (= nil response)))))

(deftest ^:integration liveness-test
  (testing "liveness endpoint"
    (let [response (liveness @test-box)]
      (is (= "OK" response)))))

(deftest ^:integration teach-model-test
  (testing "teach endpoint"
    (dotimes [n 5]
      (is (= {:success true} (teach-model @test-box model-id example))))))

(deftest ^:integration teach-model-multi-test
  (testing "teach multi endpoint"
    (dotimes [n 5]
      (is (= {:success true} (teach-model-multi @test-box model-id examples))))))

(deftest ^:integration predict-test
  (testing "predict endpoint"
    (dotimes [n 5]
      (is (= {:success true} (teach-model @test-box model-id example))))
    (let [response (predict @test-box model-id features)]
      (is (= true (:success response)))
      (is (= ["class1" "class2"] (map :id (-> response :classes))))
      (is (= '(true true) (keep #(and (> 1 %)
                                      (< 0 %))
                                (map :score (-> response :classes))))))))

(deftest ^:integration model-statistics-test
  (testing "model statistics endpoint"
    (let [response (model-statistics @test-box model-id)]
      (is (= {:success true, :predictions 0, :examples 0, :classes []}
             (model-statistics @test-box model-id)))
      (dotimes [n 5]
        (is (= {:success true} (teach-model @test-box model-id example))))
      (is (= {:success true, :predictions 0, :examples 5, :classes [{:name "class1", :examples 5}]}
             (model-statistics model-id))))))

(deftest ^:integration list-models-test
  (testing "list models endpoint"
    (let [response (list-models @test-box)]
      (is (= true (:success response)))
      (is (= 1 (count (:models response))))
      (is (= model-id (-> response :models first :id))))))

(deftest ^:integration get-model-test
  (testing "get model endpoint"
    (let [response (get-model @test-box model-id)]
      (is (= true (:success response)))
      (is (= model-id (:id response))))))

(deftest ^:integration model-and-model-id-argument-test
  (testing "using a model or a model id"
    (is (= (model-statistics @test-box model-id) (model-statistics @test-box (model ["doesn't" "matter"] :id model-id))))
    (is (= (get-model @test-box model-id) (get-model @test-box (model ["doesn't" "matter"] :id model-id))))))

(deftest ^:integration download-upload-stat-test
  (testing "state download and uploadt"
    (let [tmp-file (java.io.File/createTempFile "state" ".dat")]
      (with-open [file (io/output-stream tmp-file)]
        (.write file (download-state @test-box model-id)))
      (is (= true (:success (upload-state @test-box tmp-file))))
      (is (= true (:success (upload-state @test-box (.getAbsolutePath tmp-file)))))
      (.deleteOnExit tmp-file))))

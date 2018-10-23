(ns cljassify.client
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import java.util.Base64))

(def default-box
  {:host "localhost"
   :port 8080
   :debug false
   :body-only true})

(defn base-request
  [box]
  {:server-name (get box :host "localhost")
   :server-port (get box :port 8080)
   :debug (get box :debug false)
   :as :json
   :accept :json
   :content-type :json
   :scheme :http})

(defn- make-request
  [box request]
  (-> request client/request (cond-> (get box :body-only true) :body)))

(defn- admin
  ([endpoint] (admin endpoint default-box))
  ([endpoint box]
   (make-request box (merge (base-request box) {:method :get
                                                :uri endpoint}))))

(defn info
  "Returns information about the box."
  [& args] (apply admin "/info" args))

(defn healthz
  "Returns health information about the box."
  [& args] (apply admin "/healthz" args))

(defn readyz
  "Returns nil if the box is ready to serve requests, and fails with a 503 otherwise."
  [& args] (apply admin "/readyz" args))

(defn liveness
  "Returns the sting OK if the box is up and probably fails with a 'java.net.ConnectionException' otherwise."
  ([]
   (liveness default-box))
  ([box]
   (make-request box (merge (dissoc (base-request box) :as) {:method :get
                                                             :uri "/liveness"}))))

(defn create-model
  "Create a model (if it doesn't already exist). Takes a model of the following form:

    {
      :id \"myModelId\"
      :name \"myModelName\"
      :options {
		:ngrams 1
		:skipgrams 1
	    }
      :classes [\"class1\" \"class2\"]
    }
  "
  ([model]
   (create-model default-box model))
  ([box model]
   (make-request box (merge (base-request box) {:method :post
                                                :body (json/generate-string model)
                                                :uri "/classificationbox/models"}))))

(defn delete-model
  "Delete a model by model id."
  ([model-id]
   (delete-model default-box model-id))
  ([box model-id]
   (make-request box (merge (base-request box) {:method :delete
                                                :uri (str "/classificationbox/models/" model-id)}))))

(defn teach-model
  "Teach a model. Takes a class and a list of example features:
 
  {
    :class \"class1\" 
    :inputs [
            {
              :key \"user_age\" 
              :type \"number\" 
              :value \"32\"
            }
          ]
  }
  "
  ([model-id example]
   (teach-model default-box model-id example))
  ([box model-id example]
   (make-request box (merge (base-request box) {:method :post
                                                :body (json/generate-string example)
                                                :uri (str "/classificationbox/models/" model-id "/teach")}))))

(defn teach-model-multi
  "Teach multiple classes with a single request:
  
  {:examples
    [{:class \"class1\",
      :inputs [{:key \"user_age\", :type \"number\", :value \"25\"}]}
    {:class \"class2\",
      :inputs [{:key \"user_age\", :type \"number\", :value \"26\"}]}]}
  "
  ([model-id examples]
   (teach-model-multi default-box model-id examples))
  ([box model-id examples]
   (make-request box (merge (base-request box) {:method :post
                                                :body (json/generate-string examples)
                                                :uri (str "/classificationbox/models/" model-id "/teach-multi")}))))

(defn predict
  "Make predictions based on previously taught examples:
 
    {:limit 10, 
     :inputs [{:key \"user_age\", :type \"number\", :value \"32\"}]}
  "
  ([model-id features]
   (predict default-box model-id features))
  ([box model-id features]
   (make-request box (merge (base-request box) {:method :post
                                                :body (json/generate-string features)
                                                :uri (str "/classificationbox/models/" model-id "/predict")}))))

(defn model-statistics
  "Returns statistics about the given model."
  ([model-id]
   (model-statistics default-box model-id))
  ([box model-id]
   (make-request box (merge (base-request box) {:method :get
                                                :uri (str "/classificationbox/models/" model-id "/stats")}))))

(defn list-models
  "List the known models."
  ([]
   (list-models default-box))
  ([box]
   (make-request box (merge (base-request box) {:method :get
                                                :uri (str "/classificationbox/models")}))))
(defn get-model
  "Get a specific model by id."
  ([model-id]
   (get-model default-box model-id))
  ([box model-id]
   (make-request box (merge (base-request box) {:method :get
                                                :uri (str "/classificationbox/models/" model-id)}))))

(defn download-state
  "Download a model's state:
  
    (with-open [w (io/output-stream \"my_model.dat\")] 
      (.write w (download-state my-model-id)))
  "
  ([model-id]
   (download-state default-box model-id))
  ([box model-id]
   (make-request box (merge (base-request box) {:method :get
                                                :as :byte-array
                                                :uri (str "/classificationbox/state/" model-id)}))))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

(defn mp
  [f]
  (.encodeToString (Base64/getEncoder) (slurp-bytes (io/file f))))

(defn upload-state
  "Upload a model's state:

    (upload-state \"my_model.dat\")
  "
  ([state]
   (upload-state default-box state))
  ([box state]
   (make-request box (merge (base-request box) {:method :post
                                                :body (json/generate-string {:base64 (mp state)})
                                                :uri  "/classificationbox/state"}))))

(ns cljassify.client
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import java.util.Base64))

(defn- send-request
  [box request]
  (-> (merge {:server-name (get box :host "localhost")
              :server-port (get box :port 8080)
              :debug (get box :debug false)
              :as :json
              :method :get
              :accept :json
              :content-type :json
              :scheme :http}
             (when (and (contains? box :username)
                        (contains? box :password))
               {:basic-auth [(:username box) (:password box)]})
             request)
      client/request
      (cond-> (get box :body-only true) :body)))

(defn box
  "Create a box. When called with no arguments, the standard local options will be returned (\"localhost:8080\").
  Otherwise, specify a host and a port and, optionally:
  :username and :password for basic authentication, set :debug to true to turn on clj-http debugging,
  and set :body-only to false to return the entire response instead of just the body (also for debugging).

  Note: all client functions can be optionally called without a box parameter to just use the default box, or
  the *default-box* can be redefined.
  "
  ([]
   (box "localhost" 8080))
  ([host port & {:keys [username password debug body-only]
                 :or {username nil
                      password nil
                      debug false
                      body-only true}}]
   {:host host
    :port port
    :username username
    :password password
    :debug debug
    :body-only body-only}))

(def ^:dynamic *default-box* (box))

(defn model
  "Create a model, optionally giving it a name, id, and ngrams and/or skipgrams options. Classes should be a
  string, and a minimum of two are required."
  [classes & {:keys [name id ngrams skipgrams]
              :or {name (str (hash classes) "-n")
                   id (str (hash classes) "-i")}}]
  (when-not (sequential? classes)
    (throw (IllegalArgumentException. "Classes must be a collection.")))
  (when-not (<= 2 (count classes))
    (throw (IllegalArgumentException. "A model must have at least two classes.")))
  (-> {:name name :id id :classes classes}
      (cond->> ngrams (merge-with into {:options {:ngrams ngrams}}))
      (cond->> skipgrams (merge-with into {:options {:skipgrams skipgrams}}))))

(defn- get-model-id
  "Returns the model id if passed a model, otherwise assumes the param is a string model id."
  [m]
  (get m :id m))

(defn- admin
  ([endpoint] (admin endpoint *default-box*))
  ([endpoint box]
   (send-request box {:uri endpoint})))

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
   (liveness *default-box*))
  ([box]
   (send-request box {:uri "/liveness"
                      :as :auto})))

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
   (create-model *default-box* model))
  ([box model]
   (send-request box {:method :post
                      :body (json/generate-string model)
                      :uri "/classificationbox/models"})))

(defn delete-model
  "Delete a model by model id."
  ([model]
   (delete-model *default-box* model))
  ([box model]
   (send-request box {:method :delete
                      :uri (str "/classificationbox/models/" (get-model-id model))})))

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
  ([model example]
   (teach-model *default-box* model example))
  ([box model example]
   (send-request box {:method :post
                      :body (json/generate-string example)
                      :uri (str "/classificationbox/models/" (get-model-id model) "/teach")})))

(defn teach-model-multi
  "Teach multiple classes with a single request:

  {:examples
    [{:class \"class1\",
      :inputs [{:key \"user_age\", :type \"number\", :value \"25\"}]}
    {:class \"class2\",
      :inputs [{:key \"user_age\", :type \"number\", :value \"26\"}]}]}
  "
  ([model examples]
   (teach-model-multi *default-box* model examples))
  ([box model examples]
   (send-request box {:method :post
                      :body (json/generate-string examples)
                      :uri (str "/classificationbox/models/" (get-model-id model) "/teach-multi")})))

(defn predict
  "Make predictions based on previously taught examples:

    {:limit 10,
     :inputs [{:key \"user_age\", :type \"number\", :value \"32\"}]}
  "
  ([model features]
   (predict *default-box* model features))
  ([box model features]
   (send-request box {:method :post
                      :body (json/generate-string features)
                      :uri (str "/classificationbox/models/" (get-model-id model) "/predict")})))

(defn model-statistics
  "Returns statistics about the given model."
  ([model]
   (model-statistics *default-box* model))
  ([box model]
   (send-request box {:uri (str "/classificationbox/models/" (get-model-id model) "/stats")})))

(defn list-models
  "List the known models."
  ([]
   (list-models *default-box*))
  ([box]
   (send-request box {:uri (str "/classificationbox/models")})))

(defn get-model
  "Get a specific model by id."
  ([model]
   (get-model *default-box* model))
  ([box model]
   (send-request box {:uri (str "/classificationbox/models/" (get-model-id model))})))

(defn- read-file-bytes
  [f]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream f) out)
    (.toByteArray out)))

(defn- base64-encode
  [bytes]
  (.encodeToString (Base64/getEncoder) bytes))

(defn- encode-state
  [state]
  (condp = (class state)
    (Class/forName "[B") (base64-encode state)
    java.io.File (-> state read-file-bytes base64-encode)
    java.lang.String (-> state io/file read-file-bytes base64-encode)
    (throw (IllegalArgumentException. "State should be a byte array, a path or a file."))))

(defn upload-state
  "Upload a model's state:

    (upload-state \"my_model.dat\")
    or
    (upload-state (io/file \"my_model.dat\")
    or even
    (upload-state byte-array-that-i-made)
  "
  ([state]
   (upload-state *default-box* state))
  ([box state]
   (let [encoded (encode-state state)]
     (send-request box {:method :post
                        :body (json/generate-string {:base64 encoded})
                        :uri  "/classificationbox/state"}))))

(defn download-state
  "Download a model's state. Returns a byte array:

    (with-open [w (io/output-stream \"my_model.dat\")]
      (.write w (download-state my-model-id)))
  "
  ([model]
   (download-state *default-box* model))
  ([box model]
   (send-request box {:as :byte-array
                      :uri (str "/classificationbox/state/" (get-model-id model))})))

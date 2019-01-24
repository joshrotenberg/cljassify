(ns cljassify.dsl)

(defn input
  "Returns a Classifcationbox input for teaching or predicting."
  [key type value]
  {:key key :type type :value (str value)})

(defn example
  "Convenience function for wrapping one or more inputs as an example for teaching."
  [class & inputs]
  {:class class
   :inputs inputs})

(defn examples
  "Convenience function for wrapping multiple examples for multi-teaching."
  [& examples]
  {:examples examples})

(defn predictions
  [inputs & {:keys [limit]
             :or {limit 10}}]
  (if-not (sequential? inputs)
    {:limit limit :inputs [inputs]}
    {:limit limit :inputs inputs}))

(defmacro teach
  [model examples & body]
  `(try
     (cljassify.client/create-model ~model)
     ~@body))

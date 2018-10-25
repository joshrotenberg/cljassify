(ns cljassify.dsl)

(defn input
  [key type value]
  {:key (name key) :type (name type) :value (str value)})

(defn example
  [class & inputs]
  {:class class
   :inputs inputs})

(defn examples
  [& examples]
  {:examples examples})

(ns cljassify.dsl)

(defn model
  [classes & {:keys [name id ngrams skipgrams]
              :or {name (str (hash classes) "-n")
                   id (str (hash classes) "-i")}}]
  (-> {:name name :id id :classes classes}
      (cond->> ngrams (merge-with into {:options {:ngrams ngrams}}))
      (cond->> skipgrams (merge-with into {:options {:skipgrams skipgrams}}))))

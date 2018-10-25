# cljassify

[Machine Box][0] [Classificationbox][1] Client Library in Clojure

[![CircleCI](https://circleci.com/gh/joshrotenberg/cljassify.svg?style=svg)](https://circleci.com/gh/joshrotenberg/cljassify)

## Overview

Classificationbox is great, but I found myself really wishing I had a clean, simple way to do some exploratory
work with it, i.e. with a repl. Clojure is a fantastic language for something like this. Bam. cljassify is 
a simple, thin client for interacting with Classificationbox. It follows the API quite closely, and uses
some sane defaults to make it easy to interact with Classificationbox at the repl or write simple tools but 
should be complete enough to use for actual applications as well. 

## Usage

### Client
```clojure
;; define a model ...
(def my-model (model ["class1" "class2"]))

;; ... and create it
(create-model my-model)
;; => {:classes ["class1" "class2"]
;;     :id "1722034485-i"
;;     :name "1722034485-n"
;;     :options {}
;;     :success true}

;; create an example to teach the model
(def example {:class "class1", :inputs [{:key "user_age", :type "number", :value "32"}]})
(teach-model (:id my-model) example)
;; => {:success true}

;; now lets see if we get a prediction.
(def features {:limit 10 :inputs [{:key "user_age" :type "number" :value "32"}]})
(predict (:id my-model) features)
;; => {:classes [{:id "class1" :score 0.60721} {:id "class2" :score 0.39279}]
;;     :success true}
 
```

### DSL
```clojure
;; TBD
```

## License

Copyright © 2018 josh rotenberg

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[0]: https://machinebox.io/
[1]: https://machinebox.io/docs/classificationbox
[2]: https://machinebox.io/login?return_url=%2Faccount

# cljassify

[Machine Box][0] [Classificationbox][1] Client Library in Clojure

[![CircleCI](https://circleci.com/gh/joshrotenberg/cljassify.svg?style=svg)](https://circleci.com/gh/joshrotenberg/cljassify) [![Dependencies Status](https://versions.deps.co/joshrotenberg/cljassify/status.png)](https://versions.deps.co/joshrotenberg/cljassify) [![cljdoc badge](https://cljdoc.org/badge/cljassify/cljassify)](https://cljdoc.org/d/cljassify/cljassify/CURRENT)

[![Clojars Project](https://img.shields.io/clojars/v/cljassify.svg)](https://clojars.org/cljassify)

## Overview

cljassify is a simple, thin client for interacting with Classificationbox. It
follows the API quite closely, and uses some sane defaults to make it easy to
interact with Classificationbox at the repl or write simple tools but should be
complete enough to use for actual applications as well. 

## Installation

Lein/Boot
```
[cljassify "0.1.0-SNAPSHOT"]
```

Clojure CLI/deps.edn
```
cljassify {:mvn/version "0.1.0-SNAPSHOT"}
```

Maven
```
<dependency>
  <groupId>cljassify</groupId>
  <artifactId>cljassify</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Status

Still a work in progress.

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

## Docs

See the [uberdoc](docs/uberdoc.html).

## License

Copyright © 2018 josh rotenberg

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[0]: https://machinebox.io/
[1]: https://machinebox.io/docs/classificationbox
[2]: https://machinebox.io/login?return_url=%2Faccount

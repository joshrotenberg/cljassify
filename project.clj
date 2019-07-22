
(defproject cljassify "0.1.0-SNAPSHOT"
  :description "A Clojure client/DSL for Machinebox's Classification Box"
  :url "https://github.com/joshrotenberg/cljassify"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.9.1"]
                 [cheshire "5.8.1"]]
  :plugins [[lein-cloverage "1.0.7-SNAPSHOT"]
            [lein-cljfmt "0.6.1"]
            [lein-marginalia "0.9.1"]]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :test-selectors {:default #(not-any? % [:integration :example])
                   :integration :integration
                   :example :example}
  :profiles {:dev
             {:dependencies [[clj-http-fake "1.0.3"]
                             [org.testcontainers/testcontainers "1.11.3"]]
              :resource-paths ["test/resources"]}})

(defproject cljassify "0.1.0-SNAPSHOT"
  :description "A Clojure client/DSL for Machinebox's Clasification Box"
  :url "https://github.com/joshrotenberg/cljassify"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-http "3.9.1"]
                 [cheshire "5.8.1"]]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :example :example}
  :profiles {:test 
             {:dependencies [[clj-http-fake "1.0.3"]]}})

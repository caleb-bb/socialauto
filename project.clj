(defproject socialauto "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [etaoin "1.0.39"]
                 [environ "0.5.0"]
                 [hickory "0.7.1"]
                 [clj-http "3.12.3"]
                 [hiccup "1.0.5"]
                 ]
  :repl-options {:init-ns socialauto.core}
  :main socialauto.core
:dev-dependencies [[lein-run "1.0.0"]]
  )

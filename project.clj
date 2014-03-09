(defproject foxfight "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [info.sunng/ring-jetty9-adapter "0.6.0"]
                 [compojure "1.1.5"]
                 [hbs "0.4.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [org.clojure/tools.logging "0.2.4"]
                 [com.novemberain/monger "1.5.0"]
                 [org.clojure/data.json "0.2.2"]]
  :main foxfight.core)

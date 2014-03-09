(ns foxfight.core
  (:gen-class)
  (:use [foxfight.handler :only [app]])
  (:use [ring.adapter.jetty9 :only [run-jetty]])
  (:require [foxfight.database]
            [foxfight.ws]))

(defn -main [& args]
  (run-jetty app {:port 5990
                  :websockets {"/loc" foxfight.ws/location-handler
                               "/bat" foxfight.ws/battle-handler}}))

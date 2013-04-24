(ns foxfight.core
  (:gen-class)
  (:use [foxfight.handler :only [app]])
  (:use [ring.adapter.jetty9 :only [run-jetty]])
  (:require [foxfight.database])
  (:import [foxfight.ws.location LocationSocket]
           [foxfight.ws.battle BattleSocket]))

(defn -main [& args]
  (run-jetty app {:port 5990
                  :websockets {"/loc" LocationSocket
                               "/bat" BattleSocket}}))


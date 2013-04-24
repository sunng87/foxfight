(ns foxfight.database
  (:require [monger.core :as mg]))

(mg/connect!)
(mg/set-db! (mg/get-db "foxfight"))

(def ^:const location-collection "locations")
(def ^:const battle-collection "battles")


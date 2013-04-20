(ns foxfight.ws.location
  (:gen-class
   :name foxfight.ws.location.LocationSocket
   :init init
   :state state
   :extends org.eclipse.jetty.websocket.api.WebSocketAdapter
   :prefix ws-)
  (:use [foxfight.database :only [location-collection]])
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as logging]
            [monger.collection :as mc])
  (:import (org.eclipse.jetty.websocket.api WebSocketAdapter)
           (java.util UUID)))


(defn ws-init []
  [[] {:client-id (str (UUID/randomUUID))}])

(defn- get-client-id [this]
  (:client-id (.state this)))

(defn ws-onWebSocketConnect [this session]
  (logging/warn "new connection: " (get-client-id this))
  (-> session
      (.getRemote)
      (.sendStringByFuture
       (json/write-str {:client-id (get-client-id this)
                        :type :init}))))

(defn ws-onWebSocketText [this message]
  (let [msg (json/read-json message)
        client-id (:client-id msg)
        coords (:location msg )]
    (mc/update location-collection
               {:client-id client-id}
               {:loc {:type "Point"
                      :coordinates coords}
                :client-id client-id}
               :upsert true)))

(defn ws-onWebSocketClose [this status reason]
  (logging/warn "close socket")
  (mc/remove location-collection {:client-id (get-client-id this)}))


(ns foxfight.ws.location
  (:gen-class
   :name foxfight.ws.location.LocationSocket
   :init init
   :state state
   :extends org.eclipse.jetty.websocket.api.WebSocketAdapter
   :prefix ws-
   :exposes-methods {onWebSocketConnect superOnWebSocketConnect})
  (:use [foxfight.database :only [location-collection battle-collection]])
  (:use [foxfight.state :only [online-users battles]])
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
  (.superOnWebSocketConnect this session)
  (logging/warn "new connection: " (get-client-id this))
  (dosync
   (alter online-users assoc (get-client-id this) {:handle session
                                                   :state :idle}))
  (-> session
      (.getRemote)
      (.sendStringByFuture
       (json/write-str {:client-id (get-client-id this)
                        :type :init}))))

(defn ws-onWebSocketText [this message]
  (let [msg (json/read-json message)]
    (case (:type msg)
      "update-location" (let [client-id (:client-id msg)
                              coords (:location msg )]
                          (mc/update location-collection
                                     {:client-id client-id}
                                     {:loc {:type "Point"
                                            :coordinates coords}
                                      :client-id client-id}
                                     :upsert true))
      "request-fighting" (let [target-id (:target-client-id msg)]
                           (when-let [target-session (:handle (@online-users target-id))]
                             (dosync
                              (alter online-users update-in
                                     [(get-client-id this) :state]
                                     (constantly :waiting))
                              (alter online-users update-in
                                     [target-id :state]
                                     (constantly :requesting)))
                             (-> target-session
                                 (.getRemote)
                                 (.sendStringByFuture
                                  (json/write-str {:from-client-id (get-client-id this)
                                                   :type :fighting-confirm})))))
      "fighting-response" (let [target-id (:target-client-id msg)]
                            (when-let [target-session (:handle (@online-users target-id))]
                              ;; update state
                              (dosync
                               (alter online-users update-in
                                      [(get-client-id this) :state]
                                      (fn [_] (if (:accept msg) :in-battle :idle)))
                               (alter online-users update-in
                                      [(get-client-id this) :state]
                                      (fn [_] (if (:accept msg) :in-battle :idle))))

                              (if (:accept msg)
                                ;; start a battle
                                (let [duel-id (str (:_id (mc/insert-and-return
                                                          battle-collection
                                                          {:from target-id :to (get-client-id this)})))]
                                  (dosync
                                   (alter battles assoc duel-id {}))
                                  (-> target-session
                                      (.getRemote)
                                      (.sendStringByFuture
                                       (json/write-str {:type "battle-start"
                                                        :duel-id duel-id})))
                                  (-> this
                                      (.getRemote)
                                      (.sendStringByFuture
                                       (json/write-str {:type "battle-start"
                                                        :duel-id duel-id}))))

                                ;; battle cancelled
                                (-> target-session
                                    (.getRemote)
                                    (.sendStringByFuture
                                     (json/write-str {:type "battle-cancelled"})))))))))

(defn ws-onWebSocketClose [this status reason]
  (logging/debug "close socket")
  (dosync
   (alter online-users dissoc (get-client-id this)))
  (mc/remove location-collection {:client-id (get-client-id this)}))


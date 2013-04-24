(ns foxfight.ws.battle
  (:gen-class
   :name foxfight.ws.battle.BattleSocket
   :init init
   :state state
   :extends org.eclipse.jetty.websocket.api.WebSocketAdapter
   :prefix ws-
   :exposes-methods {onWebSocketConnect superOnWebSocketConnect})
  (:use [foxfight.state :only [battles]]
        [foxfight.database :only [battle-collection]])
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as logging]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(defn ws-init []
  [[] (atom {:bid nil})])

(defn ws-onWebSocketConnect [this session]
  (.superOnWebSocketConnect this session))

(defn ws-onWebSocketText [this message]
  (let [msg (json/read-json message)
        bid (:bid msg)]
    (case (:type msg)

      "ready"
      (do
        (swap! (.state this) assoc :bid bid)
        (when-let [battle (mc/find-one-as-map battle-collection {:_id (ObjectId. bid)})]
          (let [client-id (:client-id msg)]
            (when-let [role (cond
                             (= client-id (:from battle)) :from
                             (= client-id (:to battle)) :to)]
              (dosync
               (alter battles update-in [bid] #(assoc % role {:client-id client-id
                                                              :handle this
                                                              :blood 100})))
              (when (and (-> @battles (get bid) :from)
                         (-> @battles (get bid) :to))
                (let [resp {:from-client-id (:from battle)
                            :to-client-id (:to battle)
                            :status :start}]
                  (logging/infof "starting battle")
                  (-> @battles (get bid) :from :handle (.getRemote)
                      (.sendStringByFuture (json/write-str resp)))
                  (-> @battles (get bid) :to :handle (.getRemote)
                      (.sendStringByFuture (json/write-str resp)))))))))

      "attack"
      (let [origin (:origin msg)
            power (:power msg)
            target (keyword (if (= origin "from") "to" "from"))]
        (dosync
         (alter battles update-in [target :blood] #(- % power)))
        (let [resp {:status :engadge
                    :from-blood (-> @battles (get bid) :from :blood)
                    :to-blood (-> @battles (get bid) :to :blood)
                    :attack-origin origin}]
          (-> @battles (get bid) :from :handle (.getRemote)
              (.sendStringByFuture (json/write-str resp)))
          (-> @battles (get bid) :to :handle (.getRemote)
              (.sendStringByFuture (json/write-str resp))))))))

(defn ws-onWebSocketClose [this status reason]
  (dosync (alter battles dissoc (:bid @(.state this)))))


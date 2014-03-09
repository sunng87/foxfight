(ns foxfight.ws
  (:require [foxfight.state :refer [battles
                                    online-users]]
            [foxfight.database :refer [battle-collection
                                       location-collection]]
            [ring.adapter.jetty9 :refer :all]
            [clojure.data.json :as json]
            [clojure.tools.logging :as logging]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(def battle-handler
  {:on-text
   (fn [ws message]
     (let [msg (json/read-json message)
           bid (:bid msg)]
       (case (:type msg)

         "ready"
         (do
           (when-let [battle (mc/find-one-as-map battle-collection {:_id (ObjectId. bid)})]
             (let [client-id (:client-id msg)]
               (when-let [role (cond
                                (= client-id (:from battle)) :from
                                (= client-id (:to battle)) :to)]
                 (dosync
                  (alter battles update-in [bid] #(assoc % role {:client-id client-id
                                                                 :handle ws
                                                                 :blood 100})))
                 (when (and (-> @battles (get bid) :from)
                            (-> @battles (get bid) :to))
                   (let [resp {:from-client-id (:from battle)
                               :to-client-id (:to battle)
                               :status :start}]
                     (logging/infof "starting battle")
                     (-> @battles (get bid) :from :handle
                         (send-text (json/write-str resp)))
                     (-> @battles (get bid) :to :handle
                         (send-text (json/write-str resp)))))))))

         "attack"
         (let [origin (:origin msg)
               power (:power msg)
               target (keyword (if (= origin "from") "to" "from"))]
           (dosync
            (alter battles update-in [bid target :blood] #(- % power)))
           (let [resp {:status :engadge
                       :from-blood (-> @battles (get bid) :from :blood)
                       :to-blood (-> @battles (get bid) :to :blood)
                       :attack-origin origin}]
             (-> @battles (get bid) :from :handle
                 (send-text (json/write-str resp)))
             (-> @battles (get bid) :to :handle
                 (send-text (json/write-str resp))))))))})

(defn- client-id [ws]
  (str (.getHostString (remote-addr ws))
       ":" (.getPort (remote-addr ws))))

(def location-handler
  {:on-connect
   (fn [ws]
     (logging/info "new connection: " (client-id ws))
     (dosync
      (alter online-users assoc (client-id ws) {:handle ws
                                          :state :idle}))
     (send-text ws (json/write-str {:client-id (client-id ws)})))

   :on-text
   (fn [ws message]
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
                                        [(client-id ws) :state]
                                        (constantly :waiting))
                                 (alter online-users update-in
                                        [target-id :state]
                                        (constantly :requesting)))
                                (send-text target-session
                                           (json/write-str {:from-client-id (client-id ws)
                                                            :type :fighting-confirm}))))
         "fighting-response" (let [target-id (:target-client-id msg)]
                               (when-let [target-session (:handle (@online-users target-id))]
                                 ;; update state
                                 (dosync
                                  (alter online-users update-in
                                         [(client-id ws) :state]
                                         (fn [_] (if (:accept msg) :in-battle :idle)))
                                  (alter online-users update-in
                                         [(client-id target-session) :state]
                                         (fn [_] (if (:accept msg) :in-battle :idle))))

                                 (if (:accept msg)
                                   ;; start a battle
                                   (let [duel-id (str (:_id (mc/insert-and-return
                                                             battle-collection
                                                             {:from target-id :to (client-id ws)})))]
                                     (dosync
                                      (alter battles assoc duel-id {}))
                                     (send-text target-session
                                                (json/write-str {:type "battle-start"
                                                                 :duel-id duel-id}))
                                     (send-text ws
                                                (json/write-str {:type "battle-start"
                                                                 :duel-id duel-id})))

                                   ;; battle cancelled
                                   (send-text target-session
                                              (json/write-str {:type "battle-cancelled"}))))))))})

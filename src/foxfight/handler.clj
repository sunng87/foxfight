(ns foxfight.handler
  (:use compojure.core)
  (:use [foxfight.database :only [location-collection]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [monger.collection :as mc]
            [hbs.core :as hbs]))

(hbs/set-template-path! "/templates" ".html")

(defn json-response [d]
  {:headers {"Content-Type" "application/json; charset=UTF-8"}
   :body (json/write-str d)})

(defn index [req]
  (hbs/render-file "index" {}))

(defn handle-nearby-query [req]
  (let [{lat :lat lon :lon} (:params req)
        lat (Float/parseFloat lat)
        lon (Float/parseFloat lon)
        query {:loc {"$near" {"$geometry" {"type" "Point"
                                           "coordinates" [lon lat]}
                              "$maxDistance" 2000}}}
        results (map #(dissoc % :_id)
                     (mc/find-maps location-collection query))]
    (json-response results)))

(defn handle-battle [req]
  (hbs/render-file "battle" {:bid (-> req :params :bid)}))

(defroutes default-routes
  (GET "/" [] index)
  (GET "/location/nearby" [] handle-nearby-query)
  (GET "/battle" [] handle-battle)
  (route/resources "/"))

(def app (handler/site default-routes))




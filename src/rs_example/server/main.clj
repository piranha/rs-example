(ns rs-example.server.main
  (:gen-class)
  (:require [com.stuartsierra.component    :as c]
            [modular.ring                  :refer [WebRequestHandler]]
            [taoensso.timbre               :as log]
            [taoensso.timbre.tools.logging :refer [use-timbre]]
            [compojure.core                :as comp :refer [GET]]
            [compojure.route               :as route]
            [compojure.handler             :as handler]))

(defn basic-handler
  "An example req handler that returns a dummy application state based on the
  supplied request. In a fully featured application, this would mirror frontend
  routing (ideally through shared code)."
  [{:keys [uri] :as req}]
  {:uri uri
   :msg "This state was generated on the server!"})

(defn server-renderer
  "Takes a route->state handler function.

  Returns a wildcard Ring route for server-side rendering the frontend."
  [handler]
  (let [renderer (render/make-render-fn)]
    (GET "*" req
         (let [state (handler req)]
           {:status 200
            :headers {"Content-Type" "text/html; charset=utf-8"}
            :body (renderer state)}))))

(defn make-app [handler]
  (-> (comp/routes
        (route/resources "/")
        (server-renderer handler)
        (route/not-found "Not Found"))
    handler/site))

(defrecord WebHandler []
  c/Lifecycle
  (start [this] this)
  (stop [this] this)

  WebRequestHandler
  (request-handler [this]
    (log/debug "New request handler")
    (make-app basic-handler)))

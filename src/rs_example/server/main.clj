(ns rs-example.server.main
  (:gen-class)
  (:require [com.stuartsierra.component    :as c]
            [modular.ring                  :refer [WebRequestHandler]]
            [taoensso.timbre               :as log]
            [taoensso.timbre.tools.logging :refer [use-timbre]]
            [compojure.core                :as comp :refer [GET]]
            [compojure.route               :as route]
            [compojure.handler             :as handler]
            [rs-example.server.render      :as render]))

(defn basic-handler
  "An example req handler that returns a dummy application state based on the
  supplied request. In a fully featured application, this would mirror frontend
  routing (ideally through shared code)."
  [{:keys [uri] :as req}]
  {:uri uri
   :count 3
   :text "This state was generated on the server!"})

(defn server-renderer
  "Takes a route->state handler function.

  Returns a wildcard Ring route for server-side rendering the frontend."
  [handler render-pool]
  (GET "*" req
    (let [state (handler req)]
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (render/render render-pool state)})))

(defn make-app [handler render-pool]
  (-> (comp/routes
        (route/resources "")
        (server-renderer handler render-pool)
        (route/not-found "Not Found"))
    handler/site))

(defrecord WebHandler []
  c/Lifecycle
  (start [this] this)
  (stop [this] this)

  WebRequestHandler
  (request-handler [{:keys [render log] :as this}]
    (log/debug "New request handler")
    (make-app basic-handler render)))

(defrecord LogHandler []
  c/Lifecycle
  (start [{:keys [log-level]
           :or   {:log-level :info}
           :as   this}]
    (log/merge-config! {:timestamp-pattern "yyyy-MM-dd HH:mm:ss"})
    (log/set-level! log-level)
    (use-timbre)
    (log/info "Configured logging system")
    this)

  (stop [this] this))

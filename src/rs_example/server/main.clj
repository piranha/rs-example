(ns rs-example.server.main
  (:gen-class)
  (:require [com.stuartsierra.component    :as c]
            [taoensso.timbre               :as log]
            [taoensso.timbre.tools.logging :refer [use-timbre]]
            [modular.ring                  :refer [WebRequestHandler]]
            [ring.middleware [resource     :as rresource]
                             [file-info    :as rinfo]]
            [rs-example.server.render      :as render]))

(defn fetch-state
  "An example req handler that returns a dummy application state based on the
  supplied request. In a fully featured application, this would mirror frontend
  routing (ideally through shared code)."
  [{:keys [uri] :as req}]
  [{:text "One" :id 1}
   {:text "Two" :id 2}])

(defn server-renderer
  "Takes a route->state handler function.

  Returns a wildcard Ring route for server-side rendering the frontend."
  [state-handler render-pool]
  (fn [req]
    (let [state (state-handler req)]
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (render/render render-pool state)})))

(defn make-app [state-handler render-pool]
  (-> (server-renderer state-handler render-pool)
    (rresource/wrap-resource "")
    (rinfo/wrap-file-info)))

(defrecord WebHandler []
  c/Lifecycle
  (start [this] this)
  (stop [this] this)

  WebRequestHandler
  (request-handler [{:keys [render log] :as this}]
    (log/debug "New request handler")
    (make-app fetch-state render)))

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

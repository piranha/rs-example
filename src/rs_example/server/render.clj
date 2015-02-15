(ns rs-example.server.render
  (:import [javax.script
            Invocable
            ScriptEngineManager])
  (:require [clojure.java.io :as io]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn- render-fn* []
  (let [engine (.getEngineByName (ScriptEngineManager.) "nashorn")
        js (doto engine
             ;; React requires either "window" or "global" to be defined.
             (.eval "var global = this")
             ;; parse the compiled js file
             (.eval (-> "main.js"
                        io/resource
                        io/reader)))
        ;; eval the core namespace
        core (.eval js "rs_example.main")
        ;; pull the invocable render-to-string method out of core
        render-to-string (fn [edn]
                           (.invokeMethod ^Invocable js core
                             "render_to_string"
                             (-> edn
                               pr-str
                               list
                               object-array)))]
    (fn render [state-edn]
      (html5
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
        [:meta {:name "viewport" :content "width=device-width"}]
        [:title "Framework"]]
       [:body
        [:noscript "If you're seeing this then you're probably a search engine."]
        ;; Render view to HTML string and insert it where React will mount.
        [:div#content.container (render-to-string state-edn)]
        ;; Serialize app state so client can initialize without making an
        ;; additional request.
        [:script#state {:type "application/edn"} state-edn]
        (include-js "/framework.js")
        ;; Initialize client and pass in IDs of HTML and state elements.
        #_[:script {:type "text/javascript"}
         "rs_example.main.init('content', 'state')"]]))))

(defn make-render-fn
  "Returns a function to render fully-formed HTML.
  (fn render [title app-state-edn])"
  []
  (let [pool (ref (repeatedly 3 render-fn*))]
    (fn render [state-edn]
      (let [renderer (dosync
                       (let [f (first @pool)]
                         (alter pool rest)
                         f))
            renderer (or renderer (render-fn*))
            html (renderer state-edn)]
        (dosync (alter pool conj renderer))
        html))))

(ns rs-example.server.render
  (:import [javax.script
            Invocable
            ScriptEngineManager])
  (:require [com.stuartsierra.component :as c]
            [taoensso.timbre            :as log]
            [clojure.java.io            :as io]
            [hiccup.page                :as hi]))

(defn nashorn-env []
  (doto (.getEngineByName (ScriptEngineManager.) "nashorn")
    ;; React requires either "window" or "global" to be defined.
    (.eval "var window = this; window.location = {}; window.document = {};")
    (.eval "window.setTimeout = function() {};")
    ;; https://github.com/paulmillr/console-polyfill
    (.eval (-> "console-polyfill.js"
               io/resource
               io/reader))))

(defn bootstrap-goog [engine goog-path]
  "parse dependencies"
  (doto engine
    (.eval (-> (str goog-path "/base.js")
               io/resource
               io/reader))
    (.eval (-> (str goog-path "/deps.js")
               io/resource
               io/reader))))

(defn bootstrap-build [engine build]
  "parse the compiled js file"
  (doto engine
    (.eval (-> build
               io/resource
               io/reader))))

(defn bootstrap-dev [nashorn-env goog-path]
  (doto nashorn-env
    ;; set goog to import javascript using nashorn-env load(path)
    (.eval (str "goog.global.CLOSURE_IMPORT_SCRIPT = function(path) {
                     load('target/" goog-path "/' + path);
                     return true;
                 };"))
    ;; loop through dependencies and require to trigger injections
    #_ (.eval "for (var namespace in goog.dependencies_.nameToPath)
                goog.require(namespace);")))

(defn prepared-nashorn-env [goog-path build-path require-ns]
  (-> (nashorn-env)
    (bootstrap-goog goog-path)
    (bootstrap-dev goog-path)
    (bootstrap-build build-path)
    (doto (.eval (str "goog.require('" require-ns "');")))))

(defn nashorn-invokable [nashorn-env namespace method]
  (fn [edn]
    (.invokeMethod
      ^Invocable nashorn-env
      namespace
      method
      (-> edn
          pr-str
          list
          object-array))))

(defn nashorn-renderer [render-ns]
  (let [env (prepared-nashorn-env "out/goog" "boot-cljs-main.js" render-ns)
        namespace (.eval env render-ns)
        render-to-string (nashorn-invokable env namespace "render_to_string")]
    (with-meta (fn render [state-edn]
          (render-to-string state-edn))
      {:env env})))

(defn- render-fn* [render-ns]
  (let [render-to-string (nashorn-renderer render-ns)]
    (fn render [state-edn]
      (hi/html5
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
        [:meta {:name "viewport" :content "width=device-width"}]
        [:title "Render on Server"]]
       [:body
        ;; Render view to HTML string and insert it where React will mount.
        [:div#content.container (render-to-string state-edn)]
        ;; Serialize app state so client can initialize without making an
        ;; additional request.
        [:script#state {:type "application/edn"} (pr-str state-edn)]
        (hi/include-js "/main.js")]))))

(defprotocol IRender
  (get-render-fn [this])
  (render [this state-edn]))

(defn inc-pool! [render-pool n]
  (let [renderers (doall (repeatedly n #(get-render-fn render-pool)))]
    (dosync (alter (:pool render-pool) concat renderers))))

(defrecord RenderPool []
  c/Lifecycle
  (start [this]
    (log/info "Creating render pool")
    (let [pool (ref '())
          render-pool (assoc this :pool pool)]
      (inc-pool! render-pool (:pool-size this))
      (log/info "Created render pool")
      render-pool))

  (stop [this]
    (log/info "Stopped render pool")
    (dosync (alter (:pool this) empty))
    (dissoc this :pool))

  IRender
  (get-render-fn [this]
    (render-fn* (.replace (:render-ns this) "-" "_")))

  (render [{:keys [pool] :as this} state-edn]
    (let [renderer (dosync
                     (let [f (first @pool)]
                       (alter pool rest)
                       f))
          _ (log/debug "Got to render" renderer)
          renderer (or renderer (get-render-fn this))
          html (time (renderer state-edn))]
      (dosync (alter pool conj renderer))
      html)))

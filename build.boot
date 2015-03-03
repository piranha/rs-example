#!/usr/bin/env boot

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}

  :dependencies '[[adzerk/boot-cljs "0.0-2814-1"]
                  [adzerk/boot-cljs-repl "0.1.8" :scope "test"]
                  [adzerk/boot-reload "0.2.4" :scope "test"]
                  [pandeiro/boot-http "0.6.1" :scope "test"]
                  [jeluard/boot-notify "0.1.1" :scope "test"]
                  [deraen/boot-less "0.2.1" :scope "test"]

                  [cljsjs/react "0.12.2-5"]
                  [org.clojure/clojurescript "0.0-2850"]

                  [rum "0.2.5"]
                  [datascript "0.9.0"]
                  [tailrecursion/javelin "3.7.2"]

                  [jarohen/phoenix.runtime "0.0.4"]
                  [jarohen/phoenix.modules.aleph "0.0.1" :exclusions [aleph]]
                  [aleph "0.4.0-beta2"]
                  [ring "1.3.2"]
                  [hiccup "1.0.5"]
                  [com.taoensso/timbre "3.3.1"]])

(task-options! pom
  {:project 'rs-example
   :version "0.1.0-SNAPSHOT"})

(require
  '[adzerk.boot-cljs             :refer [cljs]]
  '[adzerk.boot-cljs-repl        :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload           :refer [reload]]
  '[pandeiro.boot-http           :refer [serve]]
  '[deraen.boot-less             :refer [less]]
  '[jeluard.boot-notify          :refer [notify]]
  '[clojure.java.io              :as io]
  '[phoenix]
  '[clojure.tools.namespace.repl :as repl])


(phoenix/init-phoenix! (io/resource "system.edn"))
(apply repl/set-refresh-dirs (get-env :directories))

(deftask start-server []
  (with-pre-wrap fs
    (phoenix/init-phoenix! (io/resource "system.edn"))
    (apply repl/set-refresh-dirs (get-env :directories))
    (#'phoenix/do-start!)
    fs))

(deftask dev
  "Start dev compiler/watcher/server"
  []
  (comp
    ;;(start-server)
    (watch)
    ;;(notify)
    (reload :on-jsload 'rs-example.main/trigger-render)
    (repl :server true)
    (cljs :source-map true
          :optimizations :none
          :compiler-options {:cache-analysis true})
    (less :source-map true)))

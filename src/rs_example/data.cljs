(ns rs-example.data
  (:require-macros [tailrecursion.javelin :refer [cell=]])
  (:require [datascript :as d]
            [tailrecursion.javelin :as j :refer [cell]]))

(extend-type j/Cell
  IWithMeta
  (-with-meta [this meta]
    (j/Cell. meta
      (.-state this) (.-rank this) (.-prev this) (.-source this)
      (.-sinks this) (.-rhunk this) (.-watches this) (.-update this))))

(def schema {})
(defonce db (with-meta (cell (d/empty-db schema))
              {:listeners (atom {})}))

(defn reset-db! []
  (reset! db (d/empty-db schema)))

(let [q '[:find [(pull ?e [:id :text]) ...]
          :where [?e :id]
          [?e :text]]]
  (def text-items (cell= (d/q q db))))


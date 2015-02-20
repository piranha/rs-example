(ns rs-example.main
  (:require-macros [tailrecursion.javelin :refer [dosync]])
  (:require [cljs.reader :refer [read-string]]
            [rum]
            [datascript :as d]
            [rs-example.data :as data]))

;; views

(rum/defc Label [item]
  (let [{:keys [id text]} item]
    [:div {:key id} (str text " ") [:span.badge id]]))

(rum/defc Root < rum/reactive []
  (let [items (rum/react data/text-items)]
    [:div
     (for [item items]
       (Label item))]))

;; server-side

(defn render-to-string [state-str]
  (let [initial (read-string state-str)]
    (dosync
      (data/reset-db!)
      (d/transact! data/db initial))
    (js/React.renderToString (Root))))

;; client-side

(defonce target (atom {:el nil
                       :comp nil}))

(defn trigger-render []
  (let [{:keys [comp el]} @target]
    (if comp
      (rum/request-render comp)
      (swap! target assoc :comp
        (rum/mount (Root) el)))))

(add-watch data/db ::render
  (fn [_ _ _ _]
    (trigger-render)))

(defn main []
  (let [initial (-> (js/document.getElementById "state")
                  .-innerHTML
                  read-string)
        el (js/document.getElementById "content")]
    (swap! target assoc :el el)
    (d/transact! data/db initial)))

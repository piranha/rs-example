(ns rs-example.main
  (:require [cljs.reader :refer [read-string]]
            [rum]
            [rs-example.data :as data]))

;; views

(rum/defc Label [n text]
  [:div
   (for [i (range n)]
     [:div {:key i} (str text " ") [:span.badge i]])])

(rum/defc Root < rum/reactive []
  (let [data (rum/react data/db)]
    (Label (:count data) (:text data))))

;; server-side

(defn render-to-string [state-str]
  (let [initial (read-string state-str)]
    (reset! data/db initial)
    (js/React.renderToString (Root))))

;; client-side

(def target (atom nil))

(defn trigger-render []
  (swap! data/db update-in [:dev] not))

(defn request-render [el state]
  (rum/mount (Root) el))

(add-watch data/db ::render
  (fn [_ _ _ state]
    (when @target
      (request-render @target state))))

(defn main []
  (let [initial (-> (js/document.getElementById "state")
                  .-innerHTML
                  read-string)
        el (js/document.getElementById "content")]
    (reset! target el)
    (reset! data/db initial)))

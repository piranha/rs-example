(ns rs-example.main
  (:require [cljs.reader :refer [read-string]]
            [rum]))

(rum/defc Label [n text]
  [:div
   (for [i (range n)]
     [:div {:key i} (str text " ") [:span.badge i]])])

(rum/defc Root [state]
  (Label (:count state) (:text state)))

(defn trigger-render []
  (rum/mount
    (Root {:count 5 :text "abcd"})
    (.getElementById js/document "content")))

(defn render-to-string [state-str]
  (let [state (read-string state-str)]
    (js/React.renderToString (Root state))))


(ns rs-example.main
  (:require [rum]))

(rum/defc Label [n text]
  [:div
   (for [i (range n)]
     [:div {:key i} (str text " ") [:span.badge i]])])

(rum/defc Root [{:keys [count text]}]
  (Label count text))

(defn trigger-render []
  (rum/mount
    (Root {:count 5 :text "abcd"})
    (.getElementById js/document "content")))

(defn render-to-string [state]
  (js/React.renderToString (Root state)))


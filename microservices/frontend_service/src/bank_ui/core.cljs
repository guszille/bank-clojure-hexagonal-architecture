(ns bank-ui.core
    (:require [reagent.dom.client :as rdomc]
              [re-frame.core :as rf]
              [bank-ui.events :as events]
              [bank-ui.subs]
              [bank-ui.routes :as routes]
              [bank-ui.views.layout :as layout]
    )
)

(defonce root (atom nil))

(defn mount-root []
    (rf/clear-subscription-cache!)
    (when (nil? @root)
        (reset! root (rdomc/create-root (.getElementById js/document "app")))
    )
    (rdomc/render @root [layout/app])
)

(defn init []
    (rf/dispatch-sync [::events/initialize-db])
    (routes/init!)
    (mount-root)
)

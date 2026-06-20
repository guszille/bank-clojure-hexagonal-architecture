(ns bank-ui.subs
    (:require [re-frame.core :as rf])
)

(rf/reg-sub ::route (fn [db _] (:route db)))
(rf/reg-sub ::accounts (fn [db _] (:accounts db)))
(rf/reg-sub ::transactions (fn [db _] (:transactions db)))
(rf/reg-sub ::investors (fn [db _] (:investors db)))
(rf/reg-sub ::issuers (fn [db _] (:issuers db)))
(rf/reg-sub ::loans (fn [db _] (:loans db)))
(rf/reg-sub ::error (fn [db _] (:error db)))

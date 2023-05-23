(ns el.ring
  (:require [el]))

(defn context-middleware
  [handler ctx]
  (fn [request]
    (-> request
        (assoc :el/context (merge {:request request} (if (fn? ctx) (ctx request) ctx)))
        (handler))))

(defn template-middleware
  [handler]
  (fn [request]
    (let [result (handler request)
          request-method (-> request :request-method)
          meta (-> request :reitit.core/match :data request-method)
          template (-> meta :el/template)
          selector (-> meta :el/selector)
          context (-> request :el/context)]
      (cond (nil? template) result
            (nil? selector) (el/template template (merge context result))
            :else (el/template template selector (merge context result))))))

(ns el.ring)

(defn context-middleware
  [ctx]
  (fn [handler]
    (fn [request]
      (-> request
          (assoc :el/context (if (fn? ctx) (ctx request) ctx))
          (handler)))))

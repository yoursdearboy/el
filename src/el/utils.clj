(ns el.utils
  (:require [clojure.string :as str]))

(defn keyword-replace [k match replacement]
  (keyword (str/replace (str k) match replacement)))

(defn keyword-starts-with? [k substr]
  (str/starts-with? (str k) (str substr)))

(defn symbolize-keys [m]
  (let [ret (persistent!
             (reduce-kv (fn [acc k v] (assoc! acc (symbol (name k)) v))
                        (transient {})
                        m))]
    (with-meta ret (meta m))))

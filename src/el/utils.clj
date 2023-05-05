(ns el.utils
  (:require [clojure.string :as str]))

(defn keyword-replace [k match replacement]
  (keyword (str/replace (str k) match replacement)))

(defn keyword-starts-with? [k substr]
  (str/starts-with? (str k) (str substr)))

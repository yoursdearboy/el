(ns el
  (:require [clojure.string :as str]
            [net.cgrand.xml :as xml]
            [net.cgrand.enlive-html :as html :refer [any-node at at* attr? attr= clone-for root set-attr]]))

(defn symbolize-keys [x]
  (into {} (for [[k v] x] [(symbol k) v])))

(def format-date-format "yyyy-MM-dd")

(defn format-date [x] (.format (java.text.SimpleDateFormat. format-date-format) x))

(defn format-value [x]
  (cond
    (inst? x) (format-date x)
    :else     (str x)))

(defn format-data [data]
  (into {} (for [[k v] data] [k (format-value v)])))

(defn replace-vars-safe
  "By default, takes a map (or function) of keywords to strings and replaces
   all occurences of ${foo} by (m :foo) in text nodes and attributes.
   Does not recurse, you have to pair it with an appropriate selector.
   re is a regex whose first group will be passed to (comp m f) and f by
   default is #'keyword."
  ([m] (replace-vars-safe #"\$\{\s*([^}]*[^\s}])\s*}" m))
  ([re m] (replace-vars-safe re m keyword))
  ([re m f]
   (let [replacement (fn [match]
                       (or (-> match second f m)
                           (-> match first)))
         substitute-vars #(str/replace % re replacement)]
     (fn [node]
       (cond
         (string? node) (substitute-vars node)
         (xml/tag? node) (assoc node :attrs
                                (into {} (for [[k v] (:attrs node)]
                                           [k (substitute-vars v)])))
         :else node)))))

(defn eval* [params code]
  (eval
   (list
    'let
    (into [] cat (-> params symbolize-keys seq))
    (read-string code))))

(defn td [row]
  (replace-vars-safe (format-data row)))

(defn tr [rows]
  (fn [match]
    ((clone-for [row rows] [:td any-node] (td row)) match)))

(defn table [params]
  (fn [match]
    (at match [:tbody :tr] (-> match :attrs :el:table keyword params tr))))

(defn if* [params]
  (fn [match]
    (if (eval* params (-> match :attrs :el:if))
      match nil)))

(defn input [value]
  (fn [match]
    (case (:tag match)
      :input ((set-attr :value value) match)
      :select (at match
                  [(attr= :value value)]
                  (set-attr :selected true)))))

(defn form [params]
  (fn [match]
    (at* match
         (for [[id value] (seq params)]
           [[(attr= :id (name id))] (input (format-value value))]))))

(defn layout [params]
  (fn [match]
    (let [source (-> match :attrs :el:layout)
          forms (for [node (html/select match [#{:head :body} :> :*])
                      :let [id (-> node :attrs :id)]
                      :when (some? id)]
                  [[(attr= :id (name id))] (html/substitute node)])]
      (at* (html/html-resource source) forms))))

(defn substitute [params]
  (fn [match]
    (let [source (-> match :attrs :el:substitute)]
      (html/html-resource source))))

(defn snippet
  ([source] (snippet source {}))
  ([source params] (snippet source [root] params))
  ([source selector params]
   ((html/snippet
     source
     selector
     []
     [(attr? :el:layout)] (layout params)
     [(attr? :el:substitute)] (substitute params)
     [(attr? :el:if)] (if* params)
     [(attr? :el:table)] (table params)
     [(attr? :el:form)] (form params)
     [any-node] (replace-vars-safe params)))))

(defn template [& args]
  (html/emit* (apply snippet args)))

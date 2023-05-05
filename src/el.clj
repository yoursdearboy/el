(ns el
  (:require [clojure.string :as str]
            [net.cgrand.xml :as xml]
            [net.cgrand.enlive-html :as html :refer [any-node at at* attr? attr= clone-for root set-attr]]
            [el.utils :refer [keyword-replace keyword-starts-with?]]))

(def ^:dynamic *options*
  {::date-format "yyyy-MM-dd"})

(defn format-date [x]
  (.format (java.text.SimpleDateFormat. (::date-format *options*)) x))

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

(defn eval- [params body]
  (let [keys (into [] (map (comp symbol name)) (keys params))
        f (eval `(fn [{:keys ~keys}] ~body))]
    (f params)))

(defn eval* [params code]
  (try (eval- params (read-string code))
       (catch clojure.lang.Compiler$CompilerException e
         (throw (ex-cause e)))))

(defn td [row]
  (replace-vars-safe (format-data row)))

(defn tr [rows]
  (fn [match]
    ((clone-for [row rows] [any-node] (td row)) (:content match))))

(defn table [params]
  (fn [match]
    (at match [:tbody] (-> match :attrs :el:table keyword params tr))))

(defn if* [params]
  (fn [match]
    (if (eval* params (-> match :attrs :el:if))
      match nil)))

(defn input [value]
  (fn [match]
    (case (:tag match)
      :input (case (-> match :attrs :type keyword)
               :checkbox (if value ((set-attr :checked value) match) match)
               ((set-attr :value (format-value value)) match))
      :textarea ((html/content (format-value value)) match)
      :select (at match
                  [(attr= :value (format-value value))]
                  (set-attr :selected true)))))

(defn form [params]
  (fn [match]
    (at* match
         (for [[id value] (seq params)]
           [[(attr= :id (name id))] (input value)]))))

(defn layout [params]
  (fn [match]
    (let [source (-> match :attrs :el:layout)
          forms (for [node (html/select match [#{:head :body} :> :*])
                      :let [id (-> node :attrs :id)]
                      :when (some? id)]
                  [[(attr= :id (name id))] (html/substitute node)])]
      (at* (html/html-resource source) forms))))

(defn str->selector [x]
  (into [] (map keyword) (clojure.string/split x #" ")))

(defn substitute [params]
  (fn [match]
    (let [source (-> match :attrs :el:substitute)
          path-selector (str/split source #" " 2)
          path (first path-selector)
          has-selector (> (count path-selector) 1)
          selector (if has-selector (str->selector (second path-selector)) [:body :> :*])]
      (-> (html/html-resource path)
          (html/select selector)))))

(defn attr-keyword-starts? [attribute-keyword]
  (html/pred
   (fn [{:keys [attrs]}]
     (and (some? attrs)
          (some #(keyword-starts-with? % attribute-keyword) (keys attrs))))))

(defn evaluate-attr? [k _]
  (and (keyword-starts-with? k :el)
       (not= k :el:layout)
       (not= k :el:substitute)
       (not= k :el:if)
       (not= k :el:table)
       (not= k :el:form)))

(defn evaluate-attrs [params]
  (map (fn [[k v]]
         (if (evaluate-attr? k v)
           [(keyword-replace k ":el:" "") (eval* params v)]
           [k v]))))

(defn evaluate [params]
  (fn [match]
    (update match :attrs (fn [attrs] (into {} (evaluate-attrs params) attrs)))))

(defn snippet
  ([source] (snippet source {}))
  ([source params] (snippet source [root] params))
  ([source selector params]
   (binding
    [*options* (merge *options* params)]
     ((html/snippet
       source
       selector
       []
       [(attr? :el:layout)] (layout params)
       [(attr? :el:substitute)] (substitute params)
       [(attr? :el:if)] (if* params)
       [(attr? :el:table)] (table params)
       [(attr? :el:form)] (form params)
       [(attr-keyword-starts? :el)] (evaluate params)
       [any-node] (replace-vars-safe (format-data params)))))))

(defn template* [& args]
  (html/emit* (apply snippet args)))

(defn template [& args]
  (apply str (apply template* args)))

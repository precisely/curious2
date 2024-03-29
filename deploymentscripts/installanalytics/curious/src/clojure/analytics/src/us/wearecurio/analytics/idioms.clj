(ns us.wearecurio.analytics.idioms
  (:require [clojure.pprint :as cp]))

(defn pp [x]
  (cp/pprint x))

(defn sort-map-by-value [m] 
    (into (sorted-map-by (fn [k1 k2] (compare (k1 m) (k2 m)))) m)) 

(defn dissoc-in [m ks]
  (let [rks (reverse ks)
        leaf (first rks)
        ks- (rest rks)]
    (update-in m (reverse ks-) dissoc leaf)))

(defn apply-to-values [f hash-map]
  (into {} (for [[k v] hash-map] [k (f v)])))

(defn apply-to-keys [f hash-map]
  (into {} (for [[k v] hash-map] [(f k) v])))

(defn collect-into-index [col index-key value-key]
  "Create an value-based index from a seq of maps.  index-key is the key whose values will be used to collect values of value-key."
  ; E.g.
  ; (def the-collection [
  ;    {:name "David" :shoe-size 9}
  ;    {:name "Roger" :shoe-size 9}
  ;    {:name "Love"  :shoe-size 3}])
  ;
  ; (collect-into-index the-collection :shoe-size :name)
  ;   => {9 ("David" "Roger")
  ;       3 ("Love")}
  ;
  (reduce (fn [accum nex]
            (let [k (get nex index-key)
                  v (get nex value-key)
                  collected (get accum k)]
              (assoc accum k (conj collected v))))
          {} col))

(defn update-in-map-series [ms ks f]
  "Given a seq of maps, transform the value at a particular key or sequence of keys."
  (map (fn [m] (update-in m ks f)) ms))

; For tapping into the -> macro for side effects.
(defn tap [x f & args]
  (apply f (cons x args))
  x)

; For tapping into the ->> macro for side effects.
(defn tap2
  ([f x] (f x) x)
  ([f arg1 x] (f arg1 x) x)
  ([f arg1 arg2 x] (f arg1 arg2 x) x))

(defn assert-type [x t]
  (assert (= t (class x))
          (str x " is not a " t ".  It's a " (class x) ".")))


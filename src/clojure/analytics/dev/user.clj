(ns user
  (require [clojure.test :as ct])
  (require [us.wearecurio.analytics.database :as db]))

(def base-ns "us.wearecurio.analytics")

(def namespaces '(binify core database idioms tag-group))

(def my-aliases {'re    'rest
                 'bi    'binify
                 'co    'core
                 'const 'constants
                 'db    'database
                 'im    'idioms
                 'iv    'interval
                 'tg    'tag-group })

(def external-aliases { 'cs 'clojure.set
                        'ic  'incanter.core
                        'kc  'korma.core
                        'ist 'incanter.stats
                        'tc  'clj-time.core
                        'tr  'clj-time.coerce })

(defn expand-ns [my-ns]
  (symbol (str base-ns "." my-ns)))

(defn full [an-ns]
  (let [name-of-ns  (get my-aliases an-ns)
        ext-ns      (get external-aliases an-ns)]
    (cond name-of-ns
            (expand-ns name-of-ns)
          ext-ns
            ext-ns
          (contains? (-> my-aliases vals set) an-ns)
            (expand-ns an-ns)
          :else
            an-ns)))

(defn make-aliases []
  (doseq [[k v] (merge my-aliases external-aliases)]
    (alias k (full v))))

(defn reload-ns [my-ns]
  (let [-my-ns (full my-ns)]
    (require -my-ns :reload)
    -my-ns))

(defn reload-test-ns [my-ns]
  (if-let [-my-ns (get my-aliases my-ns)]
    (reload-test-ns -my-ns)
    (reload-ns (symbol (str base-ns "." my-ns "-test")))))

(defn tes
  ([]
  (doseq [x (keys my-aliases)]
    (tes x)))
  ([my-ns]
  (do
    (reload-ns my-ns)
    (let [test-ns (reload-test-ns my-ns)]
      (ct/run-tests test-ns)))))

(defn res []
  (require 'user :reload)
  (let [namespaces (vals (merge my-aliases external-aliases))]
    (doseq [n (map full namespaces)]
      (require n :reload))))

(defn ret []
  (require 'us.wearecurio.analytics.binify-test :reload)
  (require 'us.wearecurio.analytics.core-test :reload)
  (require 'us.wearecurio.analytics.database-test :reload)
  (require 'us.wearecurio.analytics.tag-group-test :reload)
  (require 'us.wearecurio.analytics.test-helpers :reload))

(defn init []
  (res)
  (make-aliases)
  (db/connect))

; http://stackoverflow.com/questions/3636364/can-i-clean-the-repl
; Answered by Peter Tillemans, Sep 2010
(defn ns-clean
   "Remove all internal mappings from a given name space or the current one if no parameter given."
   ([] (ns-clean *ns*)) 
   ([ns] (map #(ns-unmap ns %) (keys (ns-interns ns)))))

(init)

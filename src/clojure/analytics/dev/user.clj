(ns user
  (require [clojure.test :as ct])
  (require [us.wearecurio.analytics.database :as db]))

(def base-ns "us.wearecurio.analytics")

(def namespaces '(binify core database idioms tag-group stats))

(def my-aliases {'re    'rest
                 'bi    'binify
                 'co    'core
                 'const 'constants
                 'db    'database
                 'im    'idioms
                 'iv    'interval
                 'dso   'datastructure-operations
                 'tg    'tag-group
                 'stats 'stats })

(def external-aliases { 'cs   'clojure.set
                        'ic   'incanter.core
                        'kc   'korma.core
                        'ist  'incanter.stats
                        'nt   'clojure.math.numeric-tower
                        'tc   'clj-time.core
                        'tr   'clj-time.coerce
                        'tf   'clj-time.format
                        'http 'org.httpkit.client })

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
  (require 'clojure.set)
  (require 'clj-time.core)
  (require 'clj-time.coerce)
  (require 'clj-time.format)
  (require 'incanter.core)
  (require 'incanter.stats)
  (require 'incanter.core)
  (require 'incanter.stats)
  (require 'clojure.math.numeric-tower)

  (require 'us.wearecurio.analytics.database :reload)
  (require 'us.wearecurio.analytics.datastructure-operations :reload)
  (require 'us.wearecurio.analytics.constants :reload)
  (require 'us.wearecurio.analytics.idioms :reload)
  (require 'us.wearecurio.analytics.binify :reload)
  (require 'us.wearecurio.analytics.interval :reload)
  (require 'us.wearecurio.analytics.rest :reload)
  (require 'us.wearecurio.analytics.stats :reload)
  (require 'us.wearecurio.analytics.tag-group :reload))


(defn ret []
  (require 'us.wearecurio.analytics.binify-test :reload)
  (require 'us.wearecurio.analytics.core-test :reload)
  (require 'us.wearecurio.analytics.database-test :reload)
  (require 'us.wearecurio.analytics.interval-test :reload)
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


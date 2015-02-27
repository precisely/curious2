(ns us.wearecurio.analytics.core
  (:gen-class :main true)
  (:require [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.interval :as iv]
            [us.wearecurio.analytics.constants :as const]
            [us.wearecurio.analytics.binify :as bi]
            [us.wearecurio.analytics.idioms :as im]
            [us.wearecurio.analytics.rest :as re]
            [clj-time.core :as tc]
            [clj-time.coerce :as tr]
            [clj-time.format :as tf]
            [clojure.math.numeric-tower :as math]
            [clojure.tools.cli :as tools]
            [clojure.java.io :as io]))


; NB: To use most functions in this namespace you must first call
;  (connect).
(defn connect []
  "A convenience wrapper for use at the REPL."
  (db/connect))

(defn parse-int [s]
  (Integer/parseInt s))

(defn parse-float [s]
  (Float/parseFloat s))

(def cli-options
  [["-a" "--all-users" "Cluster and compute correlations on all users."
    :default false]
   [nil "--alpha" "The alpha parameter the Dirichlet Process at the first epoch. A larger number means more clusters. This parameter will be lowered as the epoch count gets higher."
    :default iv/α
    :parse-fn parse-float
    :validate [#(< 0 %) "Must be a positive floating point number."]]
   [nil "--max-iter-new-cluster MAX-ITER-NEW-CLUSTER" "Maximum number of random itervals attempts to cover the data series."
    :default iv/MAX-ITER-NEW-CLUSTER
    :parse-fn parse-int
    :validate [#(< 0 %) "Must be a positive integer."]]
   [nil "--interval-size-ms INTERVAL-SIZE-MS" "The intervals size used to bin the data in order to compute the correlation."
    :default const/DAY
    :parse-fn parse-int
    :validate [#(< 0 %) "Must be a positive integer."]]
   ["-c" "--clear-log" "Delete the log file."
    :default false]
   ["-d" "--debug" "Debug option parsing."
    :default false]
   ["-e" "--max-epoch MAX-EPOCH" "Maximum number of epochs for clustering."
    :default iv/MAX-EPOCH
    :parse-fn parse-int
    :validate [#(< 0 %) "Must be a positive integer."]]
   ["-h" "--help"]
   ["-l" "--log-file-name LOG-FILE-NAME" "Log file name"
    :default const/DEFAULT-LOG-FILE]
   ["-n" "--min-n MIN-N" "Min. number of datapoints per series."
    :default iv/MIN-N
    :parse-fn parse-int
    :validate [#(< 0 %) "Must be a positive integer."]]
   ["-p" "--port PORT" "Port number"
    :default re/PORT ; 8090
    :parse-fn parse-int
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-s" "--server" "Run as server."
    :default false]
   ["-u" "--user-id USER-ID" "Cluster and compute correlations for a particular user."
    :default nil
    :parse-fn parse-int
    :validate [#(< 0 %) "Must be a positive integer."]]])

(defn print-usage-summary [parsed-options]
   (println "\nUSAGE: lein with-profile production trampoline run <OPTIONS>")
   (println (:summary parsed-options) "\n"))

(defn print-errors-if-any [parsed-options]
  (when (> (count (:errors parsed-options)) 0)
    (doseq [error (get parsed-options :errors)]
      (println error))
    (print-usage-summary parsed-options)
    (throw (Exception. "There was an error while parsing command line options."))))

(defn start-server [port]
  (println "Analytics server running on port" port)
  (re/start-server port))
 
(defn -main [ & args]
  (let [parsed-options (tools/parse-opts args cli-options)
        options (:options parsed-options)
        log-file-name (:log-file-name options)
        update-fn (fn [user-id]
                    (iv/update-user user-id
                                 :log-file-name log-file-name
                                 :max-epoch (:max-epoch options)
                                 :min-n (:min-n options)
                                 :interval-size-ms (:interval-size-ms options)
                                 :alpha (:alpha options)
                                 :max-iter-new-cluster (:max-iter-new-cluster options)))]
    (print-errors-if-any parsed-options)
    (when (:help options)
      (print-usage-summary parsed-options))
    (when (or (:server options) (:port options))
      (start-server (:port options)))
    (when (:user-id options)
      (update-fn (:user-id options)))
    (when (:all-users options)
      (doseq [user-id (db/list-user-ids)]
        (update-fn user-id)))
    (when (:clear-log options)
      (io/delete-file log-file-name))
    (when (:debug options)
      (im/pp parsed-options))))


(ns us.wearecurio.analytics.rest
  (:require [cheshire.core :as json]
            [clj-time.core :as tc]
            [clj-time.coerce :as tr]
            [org.httpkit.server :as hkit]
            [org.httpkit.client :as http]
            [korma.core :as kc]
            [net.cgrand.moustache :as mo]
            [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.interval :as iv]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(def PORT 8090)

(defonce run-state (atom nil))

; Declare the possible run states.
(def RUNNING :running)
(def IDLE-NEW :idle-new)
(def IDLE-COMPLETED :idle-completed)
(def IDLE-TERMINATED :idle-terminated)
(def IDLE-ERROR :idle-error)

(defn now []
  (tr/to-date (tc/now)))

(defn init-run-state []
  (reset! run-state
          {:status IDLE-NEW
           :updated-at nil
           :created-at (now)
           :started-at nil
           :stopped-at nil
           :notes nil
           :user-id nil
           :error nil }))

(def json-resp
  {:status 200
   :headers {"Content-type" "application/json"}
   :body ""})

(defn tag-list [user-id]
  (kc/select db/analytics_time_series
             (kc/where {:user_id user-id})
             (kc/fields :tag_id)
             (kc/fields :description)
             (kc/modifier "DISTINCT")))

(defn tag-list-json [user-id]
  (json/generate-string (tag-list user-id)))

(defn date-list-json [user-id start-date stop-date]
  (json/generate-string (iv/data-series-range-with-min-n 10 86400000 start-date stop-date user-id)))

(defn amount-list-base [user-id]
  (-> (kc/select* db/analytics_time_series)
      (kc/fields :date)
      (kc/fields :amount)
      (kc/where {:user_id user-id})
      (kc/order :date)))

(defn -amount-list
  ([user-id]
   (-> (amount-list-base user-id)
       (kc/select)))
  ([user-id tag-id]
  (-> (amount-list-base user-id)
      (kc/where {:tag_id tag-id})
      (kc/select)))
  ([user-id tag-id start-date stop-date]
  (-> (amount-list-base user-id)
      (kc/where {:tag_id tag-id})
      (kc/where (and (>= :date (tr/to-sql-time start-date))
                     (<  :date (tr/to-sql-time stop-date))))
      (kc/select))))

(defn amount-list [& args]
  (map #(update-in % [:date] db/rescale-time 86400000)
       (apply -amount-list args)))

(defn amount-list-json [& args]
  (json/generate-string (apply amount-list args)))

(defn cluster-list-json [user-id interval-size-ms]
  (json/generate-string (db/cluster-load-latest user-id interval-size-ms)))

(defn make-cluster-job-args [user-id params-map]
  (let [options (-> params-map seq flatten)]
    (cons user-id options)))

(defn start-run-state [user-id task-id]
  (let [the-run-state @run-state
        the-run-state (assoc the-run-state :status RUNNING)
        the-run-state (assoc the-run-state :updated-at (now))
        the-run-state (assoc the-run-state :started-at (now))
        the-run-state (assoc the-run-state :user-id user-id)]
    (reset! run-state the-run-state)))

(defn record-error-run-state [notes error-message]
  (let [the-run-state @run-state
        the-run-state (assoc the-run-state :status IDLE-ERROR)
        the-run-state (assoc the-run-state :updated-at (now))
        the-run-state (assoc the-run-state :stopped-at (now))
        the-run-state (assoc the-run-state :error error-message)
        the-run-state (assoc the-run-state :notes notes)]
    (reset! run-state the-run-state)))

(defn stopped-normally-run-state []
  (let [now           (now)
        the-run-state @run-state
        the-run-state (assoc the-run-state :status IDLE-COMPLETED)
        the-run-state (assoc the-run-state :updated-at now)
        the-run-state (assoc the-run-state :stopped-at now)]
    (reset! run-state the-run-state)))

(defn terminate-run-state []
  (let [now           (now)
        the-run-state @run-state
        the-run-state (assoc the-run-state :status IDLE-TERMINATED)
        the-run-state (assoc the-run-state :updated-at now)
        the-run-state (assoc the-run-state :stopped-at now)]
    (reset! run-state the-run-state)))

(defn add-line-breaks [string-seq]
  "Add line breaks to a lazy-seq of strings."
  (->> string-seq (interpose "\n") clojure.string/join))

(defmacro record-error-if-any [notes & body]
   `(try ~@body
         (catch Throwable t#
           (let [error-message#     (.getMessage t#)
                 error-cause#       (.getCause t#)
                 error-stack-trace# (.getStackTrace t#)
                 error-string# (add-line-breaks (map str error-stack-trace#))
                 error-string# (str "Caused by: " error-cause# "\n" error-string#)
                 error-string# (str "Error: " error-message# "\n" error-string#)]
             ; Record the error before throwing it again.
             (record-error-run-state ~notes error-string#)
             (throw t#)))))

(defn post-async [url query-params]
  (http/post url {:query-params query-params}))

(defn after-cluster [task-type task-id]
  (if (= "collection-child" task-type)
      (post-async "http://localhost:8080/analyticsTask/runNext" { :id task-id })
      (post-async "http://localhost:8080/analyticsTask/done" { :id task-id })))

(defn run-cluster-job [user-id req]
  (let [params (:params req)
        args (make-cluster-job-args user-id params)]
    (future (record-error-if-any "iv/update-user" (apply iv/update-user args))
            (stopped-normally-run-state)
            (after-cluster (:task-type params) (:task-id params)))))

(defn start-clustering-job-handler [user-id req]
    (init-run-state)
    (start-run-state user-id (-> req :params :task-id))
    (run-cluster-job user-id req)
    (assoc json-resp :body (json/generate-string {:message (str "begin clustering user " user-id)})))

(defn stop-job []
  "Update the run-state atom and terminate the clustering process gracefully (write the current best cluster to the dataabase)."
  (record-error-if-any "terminate-run-state" (terminate-run-state))
  (record-error-if-any "iv/terminate" (iv/terminate)))

(defn stop-job-handler [req]
  (future (stop-job))
  (assoc json-resp :body (json/generate-string {:message "stopping clustering job"})))

; ----- Server -----

(defn integer [s]
    (try (Integer/parseInt s) (catch Exception e)))

(def routes
  (mo/app

    ; Parse POST variables into a hash-map called :params in Ring's req.
    (wrap-params)
    (wrap-keyword-params)

    ["cluster" "user" [user-id integer] "run"]
      {:post (partial start-clustering-job-handler user-id)}

    ["cluster" "stop"]
      {:post stop-job-handler}

    ["cluster" "status"]
      (fn [req]
        (assoc json-resp :body (json/generate-string @run-state)))

    ; ---
   
    ["tags" [user-id integer]]
      (fn [req] (assoc json-resp :body (tag-list-json user-id)))

    ["dates" [user-id integer]
             [[_ start-year start-month start-day] #"(\d{4})-(\d{2})-(\d{2})"]
             [[_ stop-year stop-month stop-day] #"(\d{4})-(\d{2})-(\d{2})"] ]
      (fn [req]
        (let [start-time (apply db/sql-time (map integer (list start-year start-month start-day)))
              stop-time (apply db/sql-time (map integer (list stop-year stop-month stop-day)))]
          (assoc json-resp :body (date-list-json user-id start-time stop-time))))

    ["amounts" [user-id integer]]
      (fn [req] (assoc json-resp :body (amount-list-json user-id)))

    ["amounts" [user-id integer] [tag-id integer]
               [[_ start-year start-month start-day] #"(\d{4})-(\d{2})-(\d{2})"]
               [[_ stop-year stop-month stop-day] #"(\d{4})-(\d{2})-(\d{2})"] ]
      (fn [req]
        (let [start-time (apply db/sql-time (map integer (list start-year start-month start-day)))
              stop-time (apply db/sql-time (map integer (list stop-year stop-month stop-day)))]
        (assoc json-resp :body (amount-list-json user-id tag-id start-time stop-time))))

    ["clusters" [user-id integer] [interval-size-ms integer]]
      (fn [req] (assoc json-resp :body (cluster-list-json user-id interval-size-ms)))))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server [& [port]]
  (let [the-port (or port PORT)]
    (init-run-state)
    (reset! server (hkit/run-server #'routes {:port the-port}))))

(defn restart-server []
  (stop-server)
  (start-server))

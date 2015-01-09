(ns us.wearecurio.analytics.rest
  (:require [cheshire.core :as json]
            [clj-time.core :as tc]
            [clj-time.coerce :as tr]
            [org.httpkit.server :as hkit]
            [org.httpkit.client :as http]
            [korma.core :as kc]
            [environ.core :as e]
            [net.cgrand.moustache :as mo]
            [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.interval :as iv]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(def PORT 8090)
(def ENV (:env e/env))
(def WEB-URL (:web-url e/env))
(def WEB-NEXT-PATH (:web-next-path e/env))
(def WEB-DONE-PATH (:web-done-path e/env))

(defonce run-state (atom nil))

; Declare the possible run states.
(def PROCESS-RUNNING :running)
(def PROCESS-READY :ready)

; Declare possible run-sub-state
(def NEW :new)
(def CLUSTERING :clustering)
(def CORRELATING :correlations)
(def REQUEST-NEXT :request-next)
(def COMPLETED :completed) ; after finishing 1 user.
(def TERMINATED :terminated)
(def ERROR :error)

; Declare task types.  This should match what's in AnalyticsTask under
;   "Possible values for type"

(def PARENT-OF-COLLECTION 8)
(def CHILD-OF-COLLECTION 9)
(def ONE-OFF 10)

(defn now []
  (tr/to-date (tc/now)))

(defn get-status []
  (get @run-state :state))

(defn get-sub-status []
  (get @run-state :sub-state))

(defn get-error []
  (get @run-state :error))

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

(defn initial-run-state-hash [the-time]
  {:state PROCESS-READY
   :sub-state NEW
   :updated-at the-time
   :created-at the-time
   :started-at nil
   :stopped-at nil
   :user-id nil
   :task-id nil
   :error nil
   :notes nil })

(defn set-run-state [new-state]
  ;(println "BEFORE:" (:state @run-state) (:sub-state @run-state))
  ;(println " AFTER:" (:state new-state) (:sub-state new-state))
  (reset! run-state new-state))

(defn init-run-state []
  (set-run-state (initial-run-state-hash (now))))

(defn update-at [the-time]
  (-> @run-state
      (assoc :updated-at the-time)))

(defn stop-at [the-time]
  (-> (update-at the-time)
      (assoc :stopped-at the-time)))

(defn start-at [the-time]
  (-> (update-at the-time)
      (assoc :started-at the-time)))

(defn update-state-and-sub-state [the-run-state new-state sub-state]
  (-> (assoc the-run-state :state new-state)
      (assoc :sub-state sub-state)))

(defn stop-state [new-state sub-state]
  (-> (stop-at (now))
      (update-state-and-sub-state new-state sub-state)))

(defn update-state [new-state sub-state]
  (-> (update-at (now))
      (update-state-and-sub-state new-state sub-state)))

(defn start-state [new-state sub-state]
  (-> (start-at (now))
      (update-state-and-sub-state new-state sub-state)))

(defn start-state! [new-state sub-state]
  (set-run-state (start-state new-state sub-state)))

(defn stop-state! [new-state sub-state]
  (db/task-update-terminated (:task-id @run-state))
  (set-run-state (stop-state new-state sub-state)))

(defn update-state! [new-state sub-state]
  (set-run-state (update-state new-state sub-state)))

(defn start-run-state [user-id task-id]
  (db/task-update-running task-id)
  (let [the-run-state (initial-run-state-hash (now))
        the-run-state (assoc the-run-state :state PROCESS-RUNNING)
        the-run-state (assoc the-run-state :user-id user-id)
        the-run-state (assoc the-run-state :task-id task-id)]
    (set-run-state the-run-state)))

(defn record-error-run-state [notes error-message]
  (db/task-update-error (get @run-state :task-id) error-message)
  (let [the-run-state (stop-state PROCESS-READY ERROR)
        the-run-state (assoc the-run-state :error error-message)
        the-run-state (assoc the-run-state :notes notes)]
    (set-run-state the-run-state)

    ; Let the test have enough time to detect a state change.
    (when (= "test" ENV) (Thread/sleep 50))

    the-run-state))

(defn stopped-normally-run-state []
  (db/task-update-completed (get @run-state :task-id))
  (let [the-run-state (stop-state PROCESS-READY COMPLETED)]
    (set-run-state the-run-state)))

(defn completed-all-run-state []
  (let [the-run-state (stop-state PROCESS-READY COMPLETED)
        task-id       (:task-id the-run-state)
        parent-id     (db/task-parent-id task-id)]
    (when (db/task-all-successful? parent-id)
      (do (db/tasks-delete-dummy-tasks parent-id)
          (db/task-update-completed parent-id)))
    (set-run-state the-run-state)))
    
(defn terminate-run-state []
  (db/task-update-terminated (get @run-state :task-id))
  (let [the-run-state (stop-state PROCESS-READY TERMINATED)]
    (set-run-state the-run-state)))

(defn request-next-run-state []
  (let [the-run-state (update-state PROCESS-RUNNING REQUEST-NEXT)]
    (set-run-state the-run-state)))

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
             ; Record the error.
             (record-error-run-state ~notes error-string#) ; For now, don't rethrow it.  (throw t#)
             ))))

(defn post-async [url query-params & [callback]]
  (http/post url {:query-params query-params} callback))

(defn check-for-more-users [{:keys [status headers body error]}]
  (when-not (nil? body)
    (let [user-id (get (json/decode body) "userId")]
      (println "check-for-more-users")
      (println body)
      (when (nil? user-id) (completed-all-run-state)))))

;  (when error
;    (record-error-run-state "Error when requesting more users" error)))

(defn after-cluster [task-type task-id]
  (stopped-normally-run-state)
  (if (== CHILD-OF-COLLECTION task-type)
      (do
        (println "REQUEST NEXT TASK FROM WEB SERVER")
        (request-next-run-state)
        (post-async (str WEB-URL WEB-NEXT-PATH) { :id task-id :key (e/env :web-secret) } check-for-more-users))
      (do ; It's probably a ONE-OFF task, so don't request next.
        (println "ONE-OFF TASK.  STOP HERE.")
        (post-async (str WEB-URL WEB-DONE-PATH) { :id task-id :key (e/env :web-secret) }))))

(defn run-cluster-job [user-id req]
  (let [params (:params req)
        args (make-cluster-job-args user-id params)]
    (update-state! PROCESS-RUNNING CLUSTERING)
    (future (record-error-if-any "iv/update-user"
                                 (do
                                   (when (:test-throw req)
                                     (throw (Exception. "Raise error before calling update-user.")))
                                   (apply iv/update-user args)
                                   (stopped-normally-run-state)
                                   (after-cluster (Integer/parseInt (:task-type params)) (:task-id params)))))))

(defn start-clustering-job-handler [user-id req]
  (let [task-id (-> req :params :task-id)]
    (start-run-state user-id task-id)
    (db/task-update-status task-id db/TASK-RUNNING)
    ;(db/task-update-parent-status task-id db/TASK-RUNNING)
    (run-cluster-job user-id req)
    (assoc json-resp :body (json/generate-string {:message (str "begin clustering user " user-id)}))))

(defn stop-job []
  "Update the run-state atom and terminate the clustering process gracefully (write the current best cluster to the dataabase)."
  (db/task-update-parent-status (:task-id @run-state) db/TASK-TERMINATED)
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

    ["status"]
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


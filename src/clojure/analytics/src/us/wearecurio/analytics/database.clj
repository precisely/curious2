(ns us.wearecurio.analytics.database
  (:require
    [us.wearecurio.analytics.constants :as const]
    [clj-time.coerce :as tr]
    [clj-time.core :as t]
    [korma.db :as kd]
    [korma.core :as kc]
    [environ.core :as e]))

(def MIPSS_VALUE_TYPE 0)
; ***********************
; SQL time stamp wrappers
; ***********************

(defn sql-time [& args]
  (tr/to-sql-time (apply t/date-time args)))

(defn sql-now []
  (tr/to-sql-time (t/now)))

(def BIG-BANG (sql-time 1970 1 1 0 0))

(defn connection-params [environment]
  "Get the database connection parameters.  Pass in \"PRODUCTION\" \"TEST\" or \"DEVELOPMENT\" to set the database to the Grails environment's database.  Otherwise, the connection params will assume you're in a Clojure-based development environment and come from the shell's environment variables \"db_name\", \"db_user\" and \"db_password\".  These varibles are picked up by environ/env which is configured for Leiningen \"dev\" and \"test\" profiles."
  (cond (= "PRODUCTION" environment)
          {:db       "tlb"
           :user     "curious"
           :password "734qf7q35"}
        (= "TEST" environment)
          {:db       "tlb_test"
           :user     "curious"
           :password "734qf7q35"}
        (= "DEVELOPMENT" environment)
          {:db       "tlb_dev"
           :user     "curious"
           :password "734qf7q35"}
        :else
          ; Environment variables are lower-cased then dasherized into Clojure
          ;  keywords.
          ; E.g. the shell environment will change DB_NAME to :db-name.
          {:db       (e/env :db-name)
           :user     (e/env :db-user)
           :password (e/env :db-pass)}))

(defn connect
  "Connect to the database using Korma's defdb."
  ; Call with no arguments to get connection params from the shell environment
  ;  (most likely when using Leiningen but not necessarily).
  ([]       (connect (connection-params nil)))
  ([params] (kd/defdb mydb (kd/mysql params))))

; Input
(kc/defentity analytics_time_series)

; The join table for tags and tag-groups.
(kc/defentity analytics_tag_membership)

; Output
(kc/defentity analytics_tag_cluster)
(kc/defentity analytics_tag_cluster_tag)
(kc/defentity analytics_cluster_interval)

(kc/defentity analytics_cluster_run
  (kc/has-many analytics_tag_cluster))

(kc/defentity analytics_tag_cluster
  (kc/has-many analytics_tag_cluster_tag)
  (kc/belongs-to analytics_cluster_run)
  (kc/has-many analytics_cluster_interval))

(kc/defentity analytics_cluster_interval
  (kc/belongs-to analytics_tag_cluster))

(kc/defentity analytics_correlation)

; Temporary matrix store for talking to Python.
(kc/defentity analytics_cluster_input)

; **********************
; Series CRUD operations
; **********************
(defn series-list*
  "List all elements of the series."
  ([]
    (kc/select* analytics_time_series))
  ([user-id]
    (kc/where (series-list*) {:user_id user-id}))
  ([user-id tag-id]
    (kc/where (series-list* user-id) {:tag_id tag-id})))

(defn series-list [& args]
  "List all elements of the series."
  (kc/select (apply series-list* args)))

(defn series-range* [start-date stop-date & series-list-args]
  (-> (apply series-list* series-list-args)
      (kc/where (and (>= :date (tr/to-sql-time start-date))
                     (<  :date (tr/to-sql-time stop-date))))))

(defn series-range [& args]
  (-> (apply series-range* args)
      (kc/select)))

(defn model-count [base]
    (-> base
        (kc/fields ["count(*)" :count])
        (kc/select)
        first
        :count))

(defn series-count [& args]
  "Count the number of elements in the series."
  (model-count (apply series-list* args)))

(defn series-create [user-id tag-id amount date description]
  "Insert one element into the time series.  Specific to one user and one tag.  For testing purposes mainly."
  (:GENERATED_KEY (kc/insert analytics_time_series
    (kc/values {:user_id user-id
              :tag_id tag-id
              :amount amount
              :date (tr/to-sql-time date)
              :description description}))))

(defn series-delete [user-id tag-id]
  "Delete all elements of the time series.  Specific to one user and one tag.  For testing purposes mainly."
  (kc/delete analytics_time_series
    (kc/where {:user_id user-id
            :tag_id tag-id})))

; *******************
; List user's tag ids
; *******************

(defn list-tag-ids** [base & args]
  (-> (apply base args)
      (kc/fields :tag_id)
      (kc/modifier "DISTINCT")))

(defn list-tag-ids* [& args]
  "Get a distinct list of all tag ids in the database."
  (apply (partial list-tag-ids** series-list*) args))

(defn list-tag-ids-in-range* [& args]
  "Get a distinct list of all tag ids in the database."
  (apply (partial list-tag-ids** series-range*) args))

(defn map-sql-result [base mapper & args]
  (let [result (-> (apply base args)
                   (kc/select))]
     (map mapper result)))

(defn list-tag-ids [& args]
  (apply (partial map-sql-result list-tag-ids* :tag_id) args))

(defn list-tag-ids-in-range [& args]
  (apply (partial map-sql-result list-tag-ids-in-range* :tag_id) args))

(defn list-tag-ids-before-date [date user-id]
  (list-tag-ids-in-range BIG-BANG (tr/to-sql-time date) user-id))

; *****************
; List all user ids
; *****************

(defn list-user-ids []
  "Get a distinct list of user ids."
  (->> (kc/select analytics_time_series
         (kc/fields :user_id)
         (kc/modifier "DISTINCT"))
       (map #(:user_id %))))

; ****************
; Cluster run CRUD
; ****************

; Insert cluster-run with loads of reasonable default values.
(defn cluster-run-create [user-id & {:keys [start-date
                                            stop-date
                                            cluster-run-name
                                            min-n interval-size-ms]
                                     :or {start-date BIG-BANG
                                          stop-date (sql-now)
                                          cluster-run-name (str "Cluster Run " (sql-now))
                                          min-n 10
                                          interval-size-ms const/DAY}}]
  "Insert cluster run meta data."
  (:GENERATED_KEY (kc/insert analytics_cluster_run
    (kc/values {:user_id user-id
             :start_date start-date
             :stop_date stop-date
             :name cluster-run-name
             :min_n min-n
             :interval_size_ms interval-size-ms
             :created (sql-now)}))))

(defn cluster-run-get [cluster-run-id]
  "Get a specified cluster run specified by id."
  (first (kc/select analytics_cluster_run
        (kc/where {:id cluster-run-id}))))

(defn cluster-run-get-from-tag-cluster-id [tag-cluster-id]
  "Get a specified cluster run specified by id."
  (first (kc/select analytics_tag_cluster
        (kc/with analytics_cluster_run)
        (kc/where {:id tag-cluster-id}))))

; List all cluster runs (from a user).
(defn cluster-run-list
  ([]
    (kc/select analytics_cluster_run))
  ([user-id]
    (kc/select analytics_cluster_run
      (kc/where {:user_id user-id}))))

; Delete all cluster runs from a user.
(defn cluster-run-delete [user-id]
  (kc/delete analytics_cluster_run
    (kc/where {:user_id user-id})))

; List finished cluster runs from a user.
(defn cluster-run-list-finished [user-id]
  (kc/select analytics_cluster_run
    (kc/order :id :DESC)
    (kc/where {:user_id user-id})
    (kc/where {:finished [not= nil]})))

; Update a cluster run's finished field, to indicate that the run has finished.
(defn cluster-run-update-finished [id]
  (kc/update analytics_cluster_run
    (kc/set-fields {:finished (sql-now)})
    (kc/where {:id id})))

; Get the latest cluster run for a user.
(defn cluster-run-latest [user-id]
  (first (kc/select analytics_cluster_run
             (kc/order :id :DESC)
             (kc/where {:user_id user-id}))))

(defn cluster-run-latest-id [user-id]
  (:id (cluster-run-latest user-id)))

(declare tag-cluster-delete-by-cluster-run-id )
(defn cluster-run-delete [id]
   (tag-cluster-delete-by-cluster-run-id id)
   (kc/delete analytics_cluster_run (kc/where {:id id})))

(defn cluster-run-delete-many [ids]
  (doseq [id ids]
    (cluster-run-delete id)))

; Cascading delete of unfinished cluster runs.
;   "Cascading" means that it deletes everything under that has-many relationships.
(defn cluster-run-delete-unfinished
  ([]
    (let [unfinished-ids (map :id (kc/select analytics_cluster_run
                          (kc/where {:finished nil})))]
        (cluster-run-delete-many unfinished-ids)))
  ([user-id]
    (let [unfinished-ids (map :id (kc/select analytics_cluster_run
                                    (kc/where {:user_id user-id
                                                 :finished nil})))]
      (cluster-run-delete-many unfinished-ids))))

(defn cluster-run-list-old [user-id]
  (rest
    (kc/select analytics_cluster_run
      (kc/order :created :DESC)
      (kc/where {:user_id user-id}))))

(defn cluster-run-list-old-ids [user-id]
  (map :id (rest
            (kc/select analytics_cluster_run
              (kc/order :created :DESC)
              (kc/where {:user_id user-id})))))

(defn cluster-run-delete-old [user-id]
  (let [old-ids (cluster-run-list-old-ids user-id)]
    (cluster-run-delete-many old-ids)))

; *********************
; tag cluster CRUD
; *********************

; List
(defn tag-cluster-list []
  (kc/select analytics_tag_cluster))

(defn tag-cluster-list-by-cluster-run-id [cluster-run-id]
  (kc/select analytics_tag_cluster
    (kc/order :id :DESC)
    (kc/where {:analytics_cluster_run_id cluster-run-id})))

(defn tag-cluster-get [tag-cluster-id]
  (kc/select analytics_tag_cluster
    (kc/with analytics_cluster_interval)
    (kc/with analytics_tag_cluster_tag)
    (kc/where {:id tag-cluster-id})))

; List tag-ids in tag-cluster
(defn tag-cluster-list-tag-ids [tag-cluster-id]
  (map :tag_id (kc/select analytics_tag_cluster_tag
    (kc/where {:analytics_tag_cluster_id tag-cluster-id}))))

; List all tag-ids in any tag-cluster.
(defn tag-cluster-tag-list []
  (kc/select analytics_tag_cluster_tag))

; Create
(defn tag-cluster-create [cluster-run-id]
  (let [user-id (-> (cluster-run-get cluster-run-id) :user_id)]
    (:GENERATED_KEY
      (kc/insert analytics_tag_cluster
        (kc/values {:user_id user-id
                    :analytics_cluster_run_id cluster-run-id})))))


; Update (add tag to tag-cluster)
(defn tag-cluster-add-tag [tag-cluster-id tag-id loglike]
  (let [loglike-safe (if (< loglike -1000000) -1000000 loglike)]
    (:GENERATED_KEY
      (kc/insert analytics_tag_cluster_tag
        (kc/values {:tag_id tag-id
                    :analytics_tag_cluster_id tag-cluster-id
                    :loglike loglike-safe})))))

(defn tag-cluster-add-tags [tag-cluster-id members loglikes]
  (doseq [tag-id members]
    (tag-cluster-add-tag tag-cluster-id tag-id (get loglikes tag-id))))

(declare cluster-interval-save-many)
(defn tag-cluster-save-substructure [cluster-run-id state-cluster interval-size-ms make-intervals]
  (let [members           (get state-cluster :members)
        intervals         (make-intervals (get state-cluster :partition-points))
        tag-cluster-id    (tag-cluster-create cluster-run-id)
        loglikes          (get state-cluster :loglike)]
    (tag-cluster-add-tags tag-cluster-id members loglikes)
    (cluster-interval-save-many tag-cluster-id intervals interval-size-ms)))

(defn save-clusters [cluster-run-id state interval-size-ms make-intervals]
  (doseq [state-cluster (get @state :C)]
    (tag-cluster-save-substructure cluster-run-id state-cluster interval-size-ms make-intervals)))

; Delete
(defn tag-cluster-tag-delete [tag-cluster-id]
  (kc/delete analytics_tag_cluster_tag
     (kc/where {:analytics_tag_cluster_id tag-cluster-id})))

(declare cluster-interval-delete-by-tag-cluster-id)
(defn tag-cluster-delete-by-cluster-run-id [cluster-run-id]
  (doseq [tag-cluster (tag-cluster-list-by-cluster-run-id cluster-run-id)]
    ; Delete tags in the join table.
    (tag-cluster-tag-delete (get tag-cluster :id))
    ; Delete intervals in this tag-cluster.
    (cluster-interval-delete-by-tag-cluster-id (get tag-cluster :id)))
  (kc/delete analytics_tag_cluster (kc/where {:analytics_cluster_run_id cluster-run-id})))

(defn descale-time [x interval-size-in-ms]
  (tr/to-sql-time (tr/from-long (long (* x interval-size-in-ms)))))



; *********************
; cluster interval CRUD
; *********************

; List
(defn cluster-interval-list
  ([]
    (kc/select analytics_cluster_interval
      (kc/order :id :DESC)))
  ([tag-cluster-id]
    (kc/select analytics_cluster_interval
      (kc/order :id :DESC)
      (kc/where {:analytics_tag_cluster_id tag-cluster-id}))))

; Create
(defn cluster-interval-create [tag-cluster-id start-date stop-date]
  (let [user-id (:user_id (cluster-run-get-from-tag-cluster-id tag-cluster-id))]
    (:GENERATED_KEY
      (kc/insert analytics_cluster_interval
        (kc/values {:user_id user-id
                    :analytics_tag_cluster_id tag-cluster-id
                    :start_date start-date
                    :stop_date stop-date})))))

(defn cluster-interval-save-many [tag-cluster-id intervals interval-size-ms]
  (doseq [interval intervals]
    (let [iv-start-date (descale-time (first interval) interval-size-ms)
          iv-stop-date (descale-time (last interval) interval-size-ms)]
      (cluster-interval-create tag-cluster-id iv-start-date iv-stop-date))))

; Delete a specific cluster-interval.
(defn cluster-interval-delete [id]
  (kc/delete analytics_cluster_interval
    (kc/where {:id id})))

(defn cluster-interval-delete-by-tag-cluster-id [tag-cluster-id]
  (kc/delete analytics_cluster_interval
    (kc/where {:analytics_tag_cluster_id tag-cluster-id})))

; *********************
; Score CRUD operations
; *********************
(defn score-create [user-id tag1-id tag2-id score overlap-n tag1-type tag2-type]
  "Insert a correlation score."
  (:GENERATED_KEY (kc/insert analytics_correlation
    (kc/values {:user_id user-id
             :series1id tag1-id
             :series1type tag1-type
             :series2id tag2-id
             :series2type tag2-type
             :value score
             :overlapn overlap-n
             :value_type MIPSS_VALUE_TYPE
             :updated (sql-now)
             :created (sql-now)})))

(defn score-update [user-id tag1-id tag2-id score overlap-n tag1-type tag2-type]
  "Update a correlation score."
  (kc/update correlation
    (kc/set-fields {:value score
             :mipss_value score
             :overlapn overlap-n
             :updated (sql-now)
             :created (sql-now)}))))

(defn score-update [user-id tag1-id tag2-id score overlap-n tag1-type tag2-type]
  "Update a correlation score."
  (kc/update analytics_correlation
    (kc/set-fields {:mipss_value score
                 :overlapn overlap-n
                 :updated (sql-now)})
    (kc/where {:user_id    user-id
            :series1id   tag1-id
            :series1type tag1-type
            :series2id   tag2-id
            :series2type tag2-type})))

(defn score-delete [user-id tag1-id tag2-id score tag1-type tag2-type]
  "Update a correlation score."
  (kc/delete correlation
    (kc/where {:user_id    user-id
            :series1id   tag1-id
            :series1type tag1-type
            :series2id   tag2-id
            :series2type tag2-type})))


(defn score-find [user-id tag1-id tag2-id tag1-type tag2-type]
  (kc/select analytics_correlation
    (kc/where {:user_id    user-id
            :series1id    tag1-id
            :series2id    tag2-id
            :series1type  tag1-type
            :series2type  tag2-type})))

(defn score-list*
  "List all elements of the score."
  ([]
    (kc/where (score-list*) {:user_id user-id}))
  ; Add this signature when handle tag-groups:
  ;   ([user-id tag1-id tag1-type tag2-id tag2-type]
  ([user-id tag1-id tag2-id]
    (kc/where (score-list* user-id)
              {:series1id tag1-id
               :series2id tag2-id})))

(defn score-list [& args]
  "List all elements of the score."
  (kc/select (apply score-list* args)))

(defn score-count [& args]
  "Count the number of elements in the score."
  (model-count (apply score-list* args)))

; **********************
; Update or create Score
; **********************

; TODO: How will we know when to delete correlations of tags that have
;   been deleted?
(defn score-update-or-create [user-id tag1-id tag2-id score overlap-n tag1-type tag2-type]
  (let [ct (score-count user-id tag1-id tag2-id)] ; Need to update this when handling tag-groups.
    (cond (= 0 ct)
            (score-create user-id tag1-id tag2-id score overlap-n tag1-type tag2-type)
          (= 1 ct)
            (score-update user-id tag1-id tag2-id score overlap-n tag1-type tag2-type)
          :else
            (do (score-delete user-id tag1-id tag2-id score tag1-type tag2-type)
                (score-create user-id tag1-id tag2-id score overlap-n tag1-type tag2-type)))))

; ************************
; Create analytics_cluster_input (Not used, because we're not using the Python code.)
; ************************

(defn cluster-input-create [user-id group-id value]
  (:GENERATED_KEY (kc/insert analytics_cluster_input
    (kc/values {:user_id user-id
                :group_id group-id
                :value value}))))

(defn cluster-input-count []
  (model-count (kc/select* analytics_cluster_input)))

; *********
; Iterators
; *********

(defn for-all-users [f]
  "Apply a function f(user-id) to all users."
  (doseq [u (list-user-ids)]
    (f u)))

(defn for-all-tag-pairs [f user-id]
  "Apply a function f(tag1-id tag2-id) to all tag pairs."
  (doseq [t1 (list-tag-ids user-id)
          t2 (list-tag-ids user-id)]
    (f t1 t2)))

(defn for-all-users-and-tags [f]
  "Iterate over and call a function f(user-id, tag-id) on all (user-id, tag-id) permutations."
  (doseq [uid (list-user-ids)
          tid (list-tag-ids uid)]
    (f uid tid)))

(defn for-all-users-and-tag-pairs [f]
  "Iterate over and call a function f(user-id, tag1-id, tag2-id) on all (user-id, tag1-id, tag2-id) permutations."
  (doseq [uid (list-user-ids)
          t1  (list-tag-ids uid)
          t2  (list-tag-ids uid)]
    (f uid t1 t2)))

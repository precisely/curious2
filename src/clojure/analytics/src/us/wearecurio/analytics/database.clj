(ns us.wearecurio.analytics.database
  (:require
    [clj-time.coerce :as c]
    [clj-time.core :as t]
    [korma.db :as kd]
    [korma.core :as kc]
    [environ.core :as e]))

(def MIPSS_VALUE_TYPE 0)

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

; Output
(kc/defentity correlation)

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
  (kc/insert analytics_time_series
    (kc/values {:user_id user-id
              :tag_id tag-id
              :amount amount
              :date date
              :description description})))

(defn series-delete [user-id tag-id]
  "Delete all elements of the time series.  Specific to one user and one tag.  For testing purposes mainly."
  (kc/delete analytics_time_series
    (kc/where {:user_id user-id
            :tag_id tag-id})))

; *******************
; List user's tag ids
; *******************

(defn list-tag-ids [user-id]
  "Get a distinct list of all the tag ids for a given user"
  (->> (kc/select analytics_time_series
         (kc/fields :tag_id)
         (kc/modifier "DISTINCT")
         (kc/where {:user_id user-id}))
       (map #(:tag_id %))))

; *****************
; List all user ids
; *****************

(defn list-user-ids []
  "Get a distinct list of user ids."
  (->> (kc/select analytics_time_series
         (kc/fields :user_id)
         (kc/modifier "DISTINCT"))
       (map #(:user_id %))))

; ***********************
; SQL time stamp wrappers
; ***********************

(defn sql-time [& args]
  (c/to-sql-time (apply t/date-time args)))

(defn sql-now []
  (c/to-sql-time (t/now)))

; *********************
; Score CRUD operations
; *********************
(defn score-create [user-id tag1-id tag2-id score overlap-n tag1-type tag2-type]
  "Insert a correlation score."
  (kc/insert correlation
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
  (kc/select correlation
    (kc/where {:user_id    user-id
            :series1id    tag1-id
            :series2id    tag2-id
            :series1type  tag1-type
            :series2type  tag2-type})))

(defn score-list*
  "List all elements of the score."
  ([]
    (kc/select* correlation))
  ([user-id]
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
  "Iterate over and call a function f(user-id, tag1-id, tag2-id) on all (user-id, tag1-id, tag2-id) permutations)."
  (doseq [uid (list-user-ids)
          t1  (list-tag-ids uid)
          t2  (list-tag-ids uid)]
    (f uid t1 t2)))


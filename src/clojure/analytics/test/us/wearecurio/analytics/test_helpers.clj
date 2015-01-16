(ns us.wearecurio.analytics.test-helpers
  (:require [us.wearecurio.analytics.database :as db]
            [korma.core :as kc]
            [us.wearecurio.analytics.tag-group :as tg]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [environ.core :as e]
            [clojure.java.shell :as exec]))

(def TABLES '("analytics_cluster_interval"
              "analytics_cluster_run"
              "analytics_correlation"
              "analytics_tag_cluster"
              "analytics_tag_cluster_tag"
              "analytics_tag_membership"
              "analytics_task"
              "analytics_time_series"
              "entry"
              "tag"))

(defn sql-time [& args]
  (-> (apply t/date-time args) c/to-sql-time))

(def ENV (e/env :env))
(def DB-USER (e/env :db-user))
(def DB-PASS (e/env :db-pass))
(def DB-NAME (e/env :db-name))
(def DB-SCHEMA-FROM (e/env :db-schema-from))

(defmacro when-test [& body]
  `(when (= ENV "test")
    ~@body))

(defn drop-table [table-name]
  (when-test
    (kc/exec-raw (str "DROP TABLE IF EXISTS " table-name))))

(defn erase-table [table-name]
  (when-test
    (kc/exec-raw (str "DELETE FROM " table-name))))

(defn create-table [table-name]
  (let [password-arg (str "-p" DB-PASS)]
  (let [schema (-> (exec/sh "mysqldump" "-u" DB-USER password-arg "-d" DB-SCHEMA-FROM table-name)
                   :out)]
    (exec/sh "mysql" "-u" DB-USER password-arg DB-NAME :in schema))))

(defn doto-tables [f]
  (doseq [table TABLES]
    (f table)))

(defn erase-tables []
  (doto-tables erase-table))

(defn create-tables []
  (doto-tables create-table))

(defn drop-tables []
  (doto-tables drop-table))

(defn prepare-test-db []
  (do (drop-tables)
      (create-tables)))

(defn after-all []
  (drop-tables))

(defn before-each [f]
  (erase-tables)
  (f))

(defn before-all [f]
  (db/connect)
  (prepare-test-db)
  (f))



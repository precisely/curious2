(ns us.wearecurio.analytics.test-helpers
  (:require [us.wearecurio.analytics.database :as db]
            [korma.core :as kc]
            [environ.core :as e]))

(def ENV (e/env :env))

(defmacro when-test [& body]
  `(when (= ENV "test")
    ~@body))

(defn drop-table [table-name]
  (when-test
    (kc/exec-raw (str "DROP TABLE IF EXISTS " table-name))))

(defn drop-analytics-tables []
  ; Do destructive database operations only in the test environment.
  (when-test
    (do (drop-table "analytics_time_series")
        (drop-table "correlation"))))

(defn erase-table [table-name]
  (when-test
    (kc/exec-raw (str "DELETE FROM " table-name))))

(defn erase-tables []
  (erase-table "analytics_time_series")
  (erase-table "correlation"))

(defn create-table-correlation []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `correlation` (
                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                `created` datetime NOT NULL,
                `series1id` bigint(20) NOT NULL,
                `series1type` varchar(255) NOT NULL,
                `series2id` bigint(20) NOT NULL,
                `series2type` varchar(255) NOT NULL,
                `updated` datetime NOT NULL,
                `user_id` bigint(20) NOT NULL,
                `noise` datetime DEFAULT NULL,
                `saved` datetime DEFAULT NULL,
                `viewed` datetime DEFAULT NULL,
                `aux_json` varchar(255) DEFAULT NULL,
                `value` double DEFAULT NULL,
                `overlapn` double DEFAULT NULL,
                 `value_type` varchar(255) DEFAULT NULL,
                 PRIMARY KEY (`id`)
               )"))

(defn create-table-analytics-time-series []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `analytics_time_series` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `amount` decimal(19,9) DEFAULT NULL,
    `date` datetime DEFAULT NULL,
    `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
    `tag_id` bigint(20) DEFAULT NULL,
    `user_id` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `tag_id_index` (`tag_id`),
    KEY `user_id_index` (`user_id`))"))

(defn create-analytics-tables []
      (create-table-analytics-time-series)
      (create-table-correlation))

(defn prepare-test-db []
  (do (drop-analytics-tables)
      (create-analytics-tables)))

(defn init-test-db []
    (prepare-test-db))

(defn before-all []
  (println "Happy testing.")
  (db/connect)
  (init-test-db))

(defn after-all []
  (drop-analytics-tables))



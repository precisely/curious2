(ns us.wearecurio.analytics.test-helpers
  (:require [us.wearecurio.analytics.database :as db]
            [korma.core :as kc]
            [us.wearecurio.analytics.tag-group :as tg]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [environ.core :as e]))

(defn sql-time [& args]
  (-> (apply t/date-time args) c/to-sql-time))

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
        (drop-table "analytics_correlation"))))

(defn erase-table [table-name]
  (when-test
    (kc/exec-raw (str "DELETE FROM " table-name))))

(defn create-table-correlation []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `analytics_correlation` (
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

(defn erase-tables []
  ;(erase-table "tag_group_tag")
  ;(delete-in-reverse "tag_group")
  ;(erase-table "tag_group")
  ;(erase-table "tag")
  (erase-table "analytics_time_series")
  (erase-table "analytics_cluster_run")
  (erase-table "analytics_tag_cluster")
  (erase-table "analytics_tag_cluster_tag")
  (erase-table "analytics_cluster_interval")
  (erase-table "analytics_correlation")
  (erase-table "entry"))

(defn create-table-analytics-time-series []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `analytics_time_series` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `data_type` varchar(255) DEFAULT NULL,
    `amount` decimal(19,9) DEFAULT NULL,
    `date` datetime DEFAULT NULL,
    `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
    `tag_id` bigint(20) DEFAULT NULL,
    `user_id` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `tag_id_index` (`tag_id`),
    KEY `user_id_index` (`user_id`))"))

(defn create-table-cluster-run []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `analytics_cluster_run` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `finished` datetime DEFAULT NULL,
  `interval_size_ms` bigint(20) DEFAULT NULL,
  `min_n` bigint(20) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `stop_date` datetime DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `updated_index` (`updated`),
  KEY `interval_size_ms` (`interval_size_ms`),
  KEY `min_n_index` (`min_n`),
  KEY `start_date_index` (`start_date`),
  KEY `created_index` (`created`),
  KEY `user_id_index` (`user_id`),
  KEY `finished_index` (`finished`),
  KEY `stop_date_index` (`stop_date`),
  KEY `name_index` (`name`)) "))

(defn create-table-tag-cluster []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `analytics_tag_cluster` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `analytics_cluster_run_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id_index` (`user_id`),
  KEY `cluster_run_index` (`analytics_cluster_run_id`)
) "))

(defn create-table-tag-cluster-tag []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `analytics_tag_cluster_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `analytics_tag_cluster_id` bigint(20) DEFAULT NULL,
  `tag_id` bigint(20) DEFAULT NULL,
  `loglike` decimal(19,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tag_id_index` (`tag_id`),
  KEY `tag_cluster_index` (`analytics_tag_cluster_id`))"))

(defn create-table-cluster-interval []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `analytics_cluster_interval` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `start_date` datetime DEFAULT NULL,
  `stop_date` datetime DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `analytics_tag_cluster_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `start_date_index` (`start_date`),
  KEY `user_id_index` (`user_id`),
  KEY `stop_date_index` (`stop_date`),
  KEY `tag_cluster_id_index` (`analytics_tag_cluster_id`))"))

(defn create-table-entry []
  (kc/exec-raw "CREATE TABLE IF NOT EXISTS `entry` (
           `id` bigint(20) NOT NULL AUTO_INCREMENT,
           `amount` decimal(19,9) DEFAULT NULL,
           `tag_id` bigint(20) NOT NULL,
           `user_id` bigint(20) DEFAULT NULL,
           `date` datetime DEFAULT NULL,
           `units` varchar(255) NOT NULL,
           `comment` text,
           `amount_precision` int(11) DEFAULT NULL,
           `date_precision_secs` int(11) DEFAULT NULL,
           `repeat_type` int(11) DEFAULT NULL,
           `set_name` varchar(50) DEFAULT NULL,
           `tweet_id` bigint(20) DEFAULT NULL,
           `base_tag_id` bigint(20) DEFAULT NULL,
           `duration_type` int(11) DEFAULT NULL,
           `time_zone_offset_secs` int(11) DEFAULT NULL,
           `repeat_end` datetime DEFAULT NULL,
           `time_zone_id` int(11) DEFAULT NULL,
           `set_identifier` bigint(20) DEFAULT NULL,
           `group_id` bigint(20) DEFAULT NULL,
           PRIMARY KEY (`id`),
           KEY `FK5C30872CB7A9B5A` (`tag_id`),
           KEY `FK5C3087245614FBD` (`tag_id`),
           KEY `FK5C30872F07DD657` (`tag_id`),
           KEY `FK5C30872B7E2AF85` (`base_tag_id`),
           KEY `duration_type_index` (`duration_type`),
           KEY `tag_id_index` (`tag_id`),
           KEY `date_index` (`date`),
           KEY `user_id_index` (`user_id`),
           KEY `tweet_id_index` (`tweet_id`),
           KEY `repeat_type_index` (`repeat_type`),
           KEY `repeat_end_index` (`repeat_end`),
           KEY `FK5C3087272E47BD2` (`set_identifier`),
           KEY `set_identifier_index` (`set_identifier`),
           KEY `FK5C308728DD9A68B` (`group_id`),
           KEY `group_id_index` (`group_id`)
         )"))

(defn create-analytics-tables []
      (create-table-analytics-time-series)
      (create-table-correlation)
      (create-table-cluster-run)
      (create-table-tag-cluster)
      (create-table-tag-cluster-tag)
      (create-table-entry))

(defn prepare-test-db []
  (do (drop-analytics-tables)
      (create-analytics-tables)
      (create-table-cluster-interval)
      (erase-tables)))

(defn after-all []
  (drop-analytics-tables))

(defn before-each [f]
  (erase-tables)
  (f))

(defn before-all [f]
  (db/connect)
  (prepare-test-db)
  (f))



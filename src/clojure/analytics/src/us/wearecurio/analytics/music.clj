(ns us.wearecurio.analytics.music
    (:use     [incanter.stats :only [median]])
    (:require [us.wearecurio.analytics.idioms :as im])
    (:require [us.wearecurio.analytics.database :as db])
    (:require [us.wearecurio.analytics.binify :as bi]))

(def MIN-NUM-DATA-POINTS-PER-SERIES 5)
; for each user
; for each tag
; then for each tag-group
; given start-date and stop-date
; given a time-bin
; count data points
; find start date (if not given)
; find stop date (if not given)
; write to analytics_cluster_input
; call service

(db/connect)

(def user-id 4)
(def tag-id 2)
(def start-date (db/sql-time 2014 3 1))
(def stop-date (db/sql-time 2014 6 1))
(def data (db/series-list user-id tag-id))
(def avg-values (bi/binify-by-avg data))

(defn tag-label [tag-id]
  (str "tag: " tag-id " -> "))

(defn tag-type [time-binned-series]
  (-> time-binned-series vals first first :data_type))

(defn print-tag [tag-id summary]
  (println (str tag-label tag-id summary)))

(defn println-num-bins [tag-id time-binned-series]
  (print-tag tag-id (count time-binned-series)))

(defn println-first-key [tag-id time-binned-series]
  (print-tag tag-id (-> time-binned-series vals first)))

(defn println-series-type [tag-id time-binned-series ms]
  (print-tag tag-id (-> time-binned-series vals first first :data_type)))

(defn arrayify [reduced-bins start-date stop-date interval-size-ms]
  (let [start-date-index (bi/keyify start-date interval-size-ms)
        stop-date-index (bi/keyify stop-date interval-size-ms)]
    (bi/identity-series reduced-bins start-date-index stop-date-index)))

(defn counts-array [user-id tag-id interval-size-ms]
  (let [series      (db/series-list user-id tag-id)
        dates       (map :date series)
        start-date  (first dates)
        stop-date   (last dates)
        counts-bins (bi/binify-by-count series interval-size-ms)]
    (arrayify counts-bins start-date stop-date interval-size-ms)))

; Be sure to apply bi/keyify to start-date and stop-date arguments before using this function.
(defn generate-array-based-on-series-type [reducer start-date-index stop-date-index]
  (fn [tag-id time-binned-series time-interval-ms]
    (let [reduced-bins (im/apply-to-values reducer time-binned-series)
          data-type    (tag-type time-binned-series)]
      (cond (= "CONTINUOUS" data-type)
              (bi/continuous-series reduced-bins start-date-index stop-date-index)
            (= "EVENT" data-type)
              (bi/event-series reduced-bins start-date-index stop-date-index)
            :else
              (throw (Exception. (str "Unknown series type: " data-type)))))))

; Generate the array using whatever value it got mapped to regardless of the data type.
;   E.g. for raw counts you just want to know if it was recorded or not.  You don't need
;   to interpolate values if its continuous.
(defn generate-array [reducer start-date-index stop-date-index]
  (fn [tag-id time-binned-series time-interval-ms]
    (let [reduced-bins (im/apply-to-values reducer time-binned-series)]
      (bi/identity-series reduced-bins start-date-index stop-date-index))))

; The reason we're looking only at tags with data before the start date, is because
;   tags that start between the start-date and stop-date tend to cluster together
;   simply because they started between the start-date and stop-date.  So, we want to
;   create a "rectangular" block of data that so that each tag has an equal chance of
;   being clustered to the appropriate cluster without that "staircase cluster" effect.
(defn tag-ids-with-data [start-date stop-date user-id]
  (let [;tag-ids-that-start-before-start-date (-> (db/list-tag-ids-before-date start-date user-id) set)
        tag-ids-in-range (-> (db/list-tag-ids-in-range start-date stop-date user-id)
                             set)]
    ;(-> (clojure.set/intersection tag-ids-that-start-before-start-date tag-ids-in-range)
    (-> tag-ids-in-range
        sort)))

; Sample usage:
;
;  (mu/make-seq-tags-in-range mu/println-num-bins start-date stop-date 4 bi/DAY 300)
;
; INPUT:
;  `process-tag`      a function [tag-id time-slice-bins] which process all the tags in the tag-series.
;  `start-date`       a clj-time.coerce/to-sql-date start date-time that the tag entries are being
;                       selected from.
;  `stop-date`        a clj-time.coerce/to-sql-date stop date-time.
;  `user-id`          the long integer user-id.
;  `interval-size-ms` the size of the time-bins in milliseconds
;  `min-n`            the minimum number of time-slices with at least 1 data point.
(defn make-seqs-on-tags-in-range [transform-series start-date stop-date user-id interval-size-ms min-n]
    ; Instead of processing all the user's tags for every date range, process only
    ;   those tags that have at least one data point in the date range.
    (for [tag-id (tag-ids-with-data start-date stop-date user-id)]
      ; For the given tag, get all the user's data in the date range.
      (let [series (db/series-range start-date stop-date user-id tag-id)]
        ; If the number of time-slices is greater than the minimum, do something to it.
        (if-let [time-binned (bi/binify-with-min-n min-n series interval-size-ms)]
          (transform-series tag-id time-binned interval-size-ms)))))

(defn print-series [tag-id time-binned-series time-interval-ms]
  (println (str "TAGID: " tag-id "   N: " (tag-type time-binned-series))))


(defn generate-count-series [user-id start-date stop-date interval-size-ms]
  (let [start-date-index (bi/keyify start-date interval-size-ms)
        stop-date-index (bi/keyify stop-date interval-size-ms)
        gen-array-func (generate-array bi/count-bin start-date-index stop-date-index)]
    (make-seqs-on-tags-in-range gen-array-func start-date stop-date user-id interval-size-ms MIN-NUM-DATA-POINTS-PER-SERIES)))

(defn process-seqs [process-with-side-effects tag-seqs]
  (doseq [tag-seq tag-seqs]
    (when tag-seq (process-with-side-effects tag-seq))))

(defn write-to-file [output-file tag-seq]
  (let [line (str (clojure.string/join " " tag-seq) "\n")]
    (spit output-file line :append true)))

(defn delete-file [file-name]
  (spit file-name ""))

(defn write-counts-to-file [user-id start-date stop-date interval-size-ms file-name]
  (let [writer (partial write-to-file file-name)
        seqs   (generate-count-series user-id start-date stop-date interval-size-ms)]
    (do (delete-file file-name)
        (process-seqs writer seqs))))


(ns us.wearecurio.analytics.core
  (:require [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.interval :as iv]
            [us.wearecurio.analytics.constants :as const]
            [us.wearecurio.analytics.binify :as bi]
            [us.wearecurio.analytics.idioms :as im]
            [incanter.core :as ico]
            [incanter.charts :as ich]
            [incanter.stats :as ist]
            [clj-time.core :as tc]
            [clj-time.coerce :as tr]
            [clj-time.format :as cf]
            [clojure.math.numeric-tower :as math]))

; NB: To use most functions in this namespace you must first call
;  (connect).
(defn connect []
  "A convenience wrapper for use at the REPL."
  (db/connect))

(defn avg [v]
  "The average of a vector of values."
  (cond
    (nil?    v) nil
    (number? v) v
    (and (coll? v) (= 0 (count v))) nil
  :else
    (let [n   (-> (count v) double)
          sum (reduce + v)]
      (/ sum n))))

(defn sd [v]
  "The standard deviation of a bunch of numbers."
  (cond
    (number? v)
      0
    (and (coll? v) (= (count v) 0))
      nil
    (and (coll? v) (= (count v) 1))
      0
    (and (coll? v) (> (count v) 1))
      (let [mu (avg v)
            n (dec (count v))
            v2 (map (fn [x] (* (- x mu) (- x mu))) v)
            sum2 (reduce + v2)]
        (math/sqrt (/ sum2 n)))))

(defn dot-product [x y]
  (->> (interleave x y)
       (partition 2 2)
       (map #(apply * %))
       (reduce +)))

(defn normalize [x]
  (let [mu (avg x)
        s  (sd x)]
    (map #(/ (- % mu) s) x)))

(defn cor [v1 v2]
  (double (/ (dot-product (normalize v1) (normalize v2))
             (-> v1 count dec))))

; Input a sorted map, and select for keys in the range of the interval.
(defn select-in-interval [sorted interval]
    (subseq sorted >= (first interval) <= (last interval)))

(defn binify [user-id tag-id]
  (-> (db/series-list user-id tag-id)
      ;(tap println)
      (bi/binify-by-avg const/DAY)))

(defn fill-in-series [bins start-date stop-date data-type]
  (if (= "CONTINUOUS" data-type)
      (bi/continuous-series bins start-date stop-date)
      (bi/event-series bins start-date stop-date)))

(defn index-by-bin [bins start-date stop-date data-type]
  (fill-in-series bins start-date stop-date data-type))

(defn to-sorted-map [m]
  (let [filtered (filter identity m)
        vected   (vec filtered)
        flattened (flatten vected)
        sorted (apply sorted-map flattened)]
    sorted))

(defn select-values-in-intervals [filled-sorted-map intervals]
  (let [selected (map (partial select-in-interval filled-sorted-map) intervals)
        sorted (to-sorted-map selected)]
    (vals sorted)))

(defn vectorize-in-intervals [user-id tag-id start-date stop-date intervals]
  (let [data-type (db/series-data-type user-id tag-id)]
    (-> (binify user-id tag-id)
        (index-by-bin start-date stop-date data-type)
        (select-values-in-intervals intervals))))

(defn zero-cor? [x y]
  (or (< (count x) 3) (< (count y) 3)
      (== 0 (sd x)) (== 0 (sd y))))

(defn compute-correlation-helper [user-id tag1-id tag2-id intervals]
  (let [start-date (->> intervals flatten (apply min) long)
        stop-date  (->> intervals flatten (apply max) long)

        values1 (vectorize-in-intervals user-id tag1-id start-date stop-date intervals)
        values2 (vectorize-in-intervals user-id tag2-id start-date stop-date intervals)]
    (if (zero-cor? values1 values2)
        {:score 0
         :n 0
         :tag1_id tag1-id
         :tag2_id tag1-id}
        {:score (cor values1 values2)
         :n     (count values1)
         :tag1_id tag1-id
         :tag2_id tag1-id
         })))

(defn compute-correlation [user-id tag1-id tag2-id]
  (let [intervals (iv/rescaled-unioned-intervals const/DAY user-id tag1-id tag2-id)
        num-points-in-interval (-> intervals flatten count)]
    (when (> num-points-in-interval 1)
      (compute-correlation-helper user-id tag1-id tag2-id intervals))))

(defn compute-and-save-score [user-id]
  (doseq [pair (db/pairs-with-overlap-in-cluster user-id)]
    (let [tag1-id (first pair)
          tag2-id (second pair)
          result  (apply (partial compute-correlation user-id) pair)
          score   (:score result)
          n       (:n result)]
      ;(println "user-id: " user-id ", correlation: " pair " -> " score " n: " n)
      (db/score-update-or-create user-id tag1-id tag2-id score n "tag" "tag"))))

(defn update-user [user-id]
  (iv/refresh-intervals-for-user user-id)
  (compute-and-save-score user-id))


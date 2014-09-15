(ns us.wearecurio.analytics.core
  (:require [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.interval :as iv]
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

(def day-formatter (cf/formatters :year-month-day))

(defn to-day-string [entry]
  (cf/unparse day-formatter (tr/from-sql-time (:date entry))))

(defn as-day-string [entry]
  (assoc entry :date (to-day-string entry)))

(defn format-day-values [series]
  (map as-day-string series))

(defn day-values [series]
  (im/collect-into-index (format-day-values series) :date :amount))

(defn avg [v]
  "The average of a vector of values."
  (cond
    (nil?    v) nil
    (number? v) v
    (and (coll? v) (= 0 (count v))) nil
  :else
    (let [n   (* 1.0 (count v))
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

(defn series-daily-average [& args]
  "Get a sequence of [{\"YYYY-mm-dd\" <AMOUNT>} ...] where each entry represents the average amount for the day. E.g.
  {\"1986-10-04\" 40.5, \"1986-10-05\" 50.5, \"1986-10-06\" 60.5}"
  (let [series (day-values (apply db/series-list args))]
    (im/apply-to-values avg series)))

(defn standardize [x mu s]
  "Take a value and subtract the mean and divide by the standard deviation."
  (/ (- x mu) s))

(defn replace-values-with-ones [hmap]
  "Replace a hash-map's values with the floating point number 1."
  (im/apply-to-values (fn [x] 1.0) hmap))

(defn standardize-series [user-id tag-id]
  "Return the standardized series of a sequence of tags."
  ; When-let essentially checks for nil.
  (when-let [series (series-daily-average user-id tag-id)]
    ; Compute the size of the series once ahead of time.
    (let [size (count series)]
      ; Return the empty series if it's empty.
      (if (= 0 size)
        series
        (let [v  (vals series)
              mu (avg v)
              s  (sd v)]
          ; If the standard deviation is 0, the series has all
          ;   the same values, so it's essentialy a stream of
          ;   events.  So standardize events to the value of 1.
          (if (== s 0)
            (replace-values-with-ones series)
            (im/apply-to-values #(standardize % mu s) series)))))))

(defn inner-product [hmap1 hmap2]
  "Given a hash-map of (datetime-as-key, amount-as-value) entries, return the product of the values."
  (letfn [(prod [[k1 v1]]
             (if-let [v2 (get hmap2 k1)]
               (* v1 v2)))]
    (keep prod hmap1)))

(defn overlapping-keys [hmap1 hmap2]
  "Given two hashmaps, count the number of intersecting keys"
  (let [s1 (-> hmap1 keys set)
        s2 (-> hmap2 keys set)]
    (clojure.set/intersection s1 s2)))

(defn num-overlap [hmap1 hmap2]
  "Given two time-series hashes, count the number of intersecting days/time points."
  (count (overlapping-keys hmap1 hmap2)))

(defn compute-mipss [user-id tag1-id tag2-id]
  "Average product of overlapping standardized day values."
  (let [ser1  (standardize-series user-id tag1-id)
        ser2  (standardize-series user-id tag2-id)
        mipss (avg (inner-product ser1 ser2))
        n     (num-overlap ser1 ser2)]
    (when mipss
      {:mipss mipss :n n})))

(defn print-mipss [user-id tag1-id tag2-id score]
  (println user-id " " tag1-id " " tag2-id " " (score :mipss) " " (score :n)))

(defn compute-and-save-score [user-id tag1-id tag2-id]
	(when-let [mipss					(compute-mipss user-id tag1-id tag2-id)]
		(let [score					(mipss :mipss)
          overlap-n     (mipss :n)]
			;(print-mipss user-id tag1-id tag2-id mipss)
			(db/score-update-or-create user-id tag1-id tag2-id score overlap-n "tag" "tag"))))

(defn update-all-users []
	"For each user in anaytics_time_series, iterate over all that user's tag-pairs, compute the MIPSS, then save it to the correlation table."
	(db/for-all-users-and-tag-pairs compute-and-save-score))

(defn update-user [user-id]
  (iv/refresh-intervals-for-user user-id)
	(db/for-all-tag-pairs (partial compute-and-save-score user-id) user-id))

(defn update-all-users []
  "For each user in anaytics_time_series, iterate over all that user's tag-pairs, compute the MIPSS, then save it to the correlation table."
  (db/for-all-users iv/refresh-intervals-for-user)
  (db/for-all-users-and-tag-pairs compute-and-save-score))

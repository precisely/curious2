(ns us.wearecurio.analytics.binify
  (:require [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.idioms :as im]
            [us.wearecurio.analytics.constants :as const]
            [clj-time.coerce :as tr]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.math.numeric-tower :as nt]))

(def HOUR const/HOUR)
(def DAY  const/DAY)

; E.g. (date-time 1986 10 14 4 3 27)
(defn date-time [& args]
  "This just wraps clj-time/date-time to expose a standard interface for
making date-time objects that are compatible with keyify."
  (apply t/date-time args))

(defn keyify
  ([date-time]
    (keyify date-time DAY))
  ([date-time interval-size-ms]
    (-> (/ (tr/to-long date-time) interval-size-ms)
        clojure.math.numeric-tower/floor)))

(defn append-datum [interval-size-ms]
  (fn [bins point]
    (let [date-time (:date point)
          k         (keyify date-time interval-size-ms)]
      (update-in bins [k] #(cons point %)))))

(defn binify
  ([series]
    (binify series DAY))
  ([series interval-size-ms]
    (reduce (append-datum interval-size-ms) {} series)))

(defn binify-with-min-n [min-n & binify-args]
  (let [binned (apply binify binify-args)]
    (if (>= (count binned) min-n)
        binned
        nil)))

(defn count-bin [bin]
  (count bin))

(defn max-bin [bin]
  (reduce max (map :amount bin)))

(defn sum-bin [bin]
  (reduce + (map :amount bin)))

(defn avg-bin [bin]
  (/ (reduce + (map :amount bin)) (double (count bin))))

(defn map-bins [f & args]
  (let [time-bins (apply binify args)]
    (im/apply-to-values f time-bins)))

(def binify-by-count (partial map-bins count-bin))

(def binify-by-avg (partial map-bins avg-bin))

(def binify-by-max (partial map-bins max-bin))

(defn last-x [bins]
  (-> bins keys sort last))

(defn last-y [bins]
  (get bins (last-x bins)))

(defn longify-keys [m]
  (into {} (zipmap (map long (keys m))
                   (vals m))))

; Generate sequences with start-dates and stop-dates after the binification process.
; value-series-map looks like
; {1160 7.7 16161 6.5 16162 7.0}, where the keys are the keyified dates,
;   and the bin values have already been averaged, maxed, summed or whatever.
(defn event-series
  [value-series-map start-date-index stop-date-index]
  (let [zero-keys    (map long (range start-date-index (inc stop-date-index)))
        zeroes       (repeat (count zero-keys) 0)
        zero-series  (apply sorted-map (interleave zero-keys zeroes))
        value-series (->> (longify-keys value-series-map)
                           vec flatten (apply sorted-map))]
    (merge zero-series value-series)))

; event-series just returns the value if it exists or 0 otherwise, so let's just call it the identity series
;  generator.
(defn identity-series [& args]
  (apply event-series args))

; ordered-date-index-pairs is a sequence of pair of date-indices that have user-entered data (y-values).
; E.g. (partition 2 1 (-> value-series-map keys sort))
(defn -continuous-series [value-series-map ordered-date-index-pairs current-date-index stop-date-index last-data-point-x last-data-point-y]
  (let [current-value (get value-series-map current-date-index)
        x0            (-> ordered-date-index-pairs first first)
        y0            (get value-series-map x0)
        x1            (second (first ordered-date-index-pairs))
        y1            (get value-series-map x1)]
    (cond
      ; Terminate after the stop-date.
      (> current-date-index stop-date-index) '()

      ; After last data point.
      (or (nil? x1) (> current-date-index last-data-point-x))
          (cons last-data-point-y (lazy-seq (-continuous-series value-series-map nil (inc current-date-index) stop-date-index last-data-point-x last-data-point-y)))

      ; Before reaching first data point.
      (< current-date-index x0)
        (cons y0 (lazy-seq (-continuous-series value-series-map ordered-date-index-pairs (inc current-date-index) stop-date-index last-data-point-x last-data-point-y)))

      ; Lop off heads until you reach the relevant interval.
      (> current-date-index x1)
        (-continuous-series value-series-map (rest ordered-date-index-pairs) current-date-index stop-date-index last-data-point-x last-data-point-y)

      ; We found a value at the current time slice.
      current-value
        (let [ordered-date-index-pairs2 (if (== current-date-index x1) (rest ordered-date-index-pairs) ordered-date-index-pairs)]
              (cons current-value (lazy-seq (-continuous-series value-series-map ordered-date-index-pairs2 (inc current-date-index) stop-date-index last-data-point-x last-data-point-y))))
      ; The point lies between 
      (and (> current-date-index x0) (< current-date-index x1))
        (let [m       (/ (double (- y1 y0)) (double (- x1 x0)))
              Δx      (- current-date-index x0)
              y       (+ y0 (* Δx m))]
          (cons y (lazy-seq (-continuous-series value-series-map ordered-date-index-pairs (inc current-date-index) stop-date-index last-data-point-x last-data-point-y))))
      :else
        (throw (Exception. "This shouldn't happen")))))

(defn continuous-series
  "A convenience wrapper for -continuous-series."
  [value-series-map start-date-index stop-date-index]
  (let [last-data-point-x (-> value-series-map keys sort last)
        last-data-point-y (last-y value-series-map)
        date-index-pairs  (partition 2 1 (-> value-series-map keys sort))
        ; start-date-index, stop-date-index and the keys of value-series-map
        ;   need to be of the same type so that the look-up by key value will work.
        value-series-map  (im/apply-to-keys long value-series-map)
        start-date-index  (long start-date-index)
        stop-date-index   (long stop-date-index)]
    (->> (-continuous-series value-series-map date-index-pairs start-date-index stop-date-index last-data-point-x last-data-point-y)
        (interleave (range start-date-index (inc stop-date-index)))
        (apply sorted-map))))


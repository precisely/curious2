(ns us.wearecurio.analytics.detect-changes
  (:require
     [incanter.stats :as ist]
     [us.wearecurio.analytics.idioms :as im]
     [us.wearecurio.analytics.stats :as stats]
     [us.wearecurio.analytics.constants :as const]))

(def MIN-SAMPLE-SIZE 4)
(def WINDOW-SIZE (* 3 MIN-SAMPLE-SIZE))
(def SIGNIFICANCE-LEVEL 0.001)
; Basic idea:
; Write a function that takes a vector and returns the index of the change points.
; E.g.
; INPUT: [0 0 0 0 0 1 1 1 1 1 1] would return 4.5

;(defn part [v n]
;  "Partition a sequence v into two subsequences.  The first subsequence is of length n.  Outputs a list of lists."
;  (list (take n v)
;        (take-last (- (count v) n) v)))
;
;(defn partition-lengths [v]
;  (let [min-n MIN-SAMPLE-SIZE]
;    (range min-n (- (count v) (dec min-n)))))
;
(defn perturb [v]
  "Perturb a vector of numbers if they're all the same in order to get a non-zero standard deviation."
  (if (== 0 (ist/sd v))
      (cons (+ 0.01 (first v)) (rest v))
      v))

(defn get-p-value [v1 v2]
  (let [v1' (perturb v1)
        v2' (perturb v2)]
  (get (ist/t-test v1' :y v2') :p-value)))

(defn partitions-for-t-test [v]
  (->> v
       (partition (* 2 MIN-SAMPLE-SIZE) 1)
       (map #(partition MIN-SAMPLE-SIZE MIN-SAMPLE-SIZE %))))

(defn get-p-values [v]
  (map #(apply get-p-value %) (partitions-for-t-test v)))

(defn get-min-index
  ([nums]
  "Get the index of the max value of a list of numbers."
  (+ MIN-SAMPLE-SIZE -1
     (->> nums
       (interleave (range (count nums)))
       (partition 2 2)
       (sort-by last)
       first
       first))))

(defn change-point-index [v & {:keys [offset] :or {offset 0}}]
  "Get the index of the last value before the most extreme change point in a list of numbers."
  ; Return nil if there are fewer than 2 X MIN-SAMPLE-SIZE numbers in the list.
  (when (> (count v) (* 2 (dec MIN-SAMPLE-SIZE)))
    (let [v'     (-> v perturb stats/normalize)
          p-vals (get-p-values v')
          min-p-val (if (> (count p-vals) 0) (apply min p-vals) 1)]
      (when (< min-p-val SIGNIFICANCE-LEVEL)
            (+ offset (get-min-index p-vals))))))

(defn change-point-value [v]
  (get (vec v) (change-point-index v)))

(defn window-for-remainder [v]
  (let [remainder   (mod (count v) MIN-SAMPLE-SIZE)]
    (when (not= 0 remainder)
      (list (take-last (+ (* 2 MIN-SAMPLE-SIZE) remainder) v)))))

(defn partitions-for-change-points [v]
  (partition WINDOW-SIZE MIN-SAMPLE-SIZE v))
    
(defn change-point-indexes-helper [v]
  "Input a vector and return the index representing the point where there was a significant change in values."
  (let [N (count v)]
    (cond (< N (* 2 MIN-SAMPLE-SIZE)) (repeat (count v) 0)
          (< N (* 3 MIN-SAMPLE-SIZE)) (list (change-point-index v))
          :else (map #(apply change-point-index %)
                     (partition 3 3 (interleave (partitions-for-change-points v)
                                                (repeat (long (/ (count v) MIN-SAMPLE-SIZE)) :offset)
                                                (range 0 (count v) MIN-SAMPLE-SIZE)))))))

(defn change-point-indexes [v]
  (->> v change-point-indexes-helper (filter identity) (apply sorted-set)))

(defn change-point-spike-train [v]
  (let [zeroes (vec (repeat (count v) 0))]
    (->> (change-point-indexes v)
         (reduce #(assoc %1 %2 1)
                 (vec (repeat (count v) 0))))))



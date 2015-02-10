(ns us.wearecurio.analytics.stats
  (:require [clojure.math.numeric-tower :as nt]))

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
        (nt/sqrt (/ sum2 n)))))

(defn dot [x y]
  (->> (interleave x y)
       (partition 2 2)
       (map #(apply * %))))

(defn dot-product [x y]
  (->> (dot x y)
       (reduce +)))

(defn normalize [x]
  (let [mu (avg x)
        s  (sd x)]
    (map #(/ (- % mu) s) x)))

(defn sub [x y]
  (->> (interleave x y)
       (partition 2 2)
       (map #(apply - %))))

(defn cor [v1 v2]
  (double (/ (dot-product (normalize v1) (normalize v2))
             (-> v1 count dec))))

(defn dist [x y]
  (->>  (sub x y)
        (map #(nt/expt % 2))
        (sort-by nt/abs) ; keep errors small by added up small stuff first.
        (reduce +))) 
       
; ----- Generate step functions for testing.
(defn step-function [& args]
  "E.g. (step-function 3 0 5 1)
  => (0 0 0 1 1 1 1 1)"
  (flatten (map #(apply repeat %) (partition 2 2 args))))

(defn pad-n [s n]
  "Pad the left and right of a sequence with repeated n-length edges of the sequence s itself."
  (let [h  (->> (cycle s) ; cycle to ensure you can always take n elements when n > (count s).
                (take n)
                reverse)
        t  (->> (reverse s)
                cycle
                (take n))]
    (concat h s t)))

; ----- Kernel smoothing of a sequence, where the base kernel is [1/3 1/3 1/3] when n=1
; -----  n is the number of times to convolve the base kernel.
;

(defn smooth-helper [s n]
  (if (= n 0)
    s
    (recur (map avg (partition 3 1 s)) (dec n))))

(defn smooth [s n]
  (let [h  (->> (cycle s) ; cycle to ensure you can always take n elements when n > (count s).
                (take n)
                reverse)
        t  (->> (reverse s)
                cycle
                (take n))
        s2 (concat h s t)]
    (smooth-helper (pad-n s n) n)))

(defn laplacian [s]
  (->> (map #(dot % '(1 -2 1)) (partition 3 1 s))
       (sort-by nt/abs)
       (map #(reduce + %))))

(defn sln [r]
  (-> r (smooth 3) laplacian normalize))

(defn dsmooth [x y]
  (dist (smooth x 5) (smooth y 5)))

; (use '(incanter core stats charts io))
; (defn graph [d] (with-data (dataset [:x :y ] (partition 2 2 (interleave (range (count d)) d))) (view (scatter-plot :x :y))))

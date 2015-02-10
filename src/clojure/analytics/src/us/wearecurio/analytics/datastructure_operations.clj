(ns us.wearecurio.analytics.datastructure-operations
  (:require [us.wearecurio.analytics.stats :as stats]))

(defn no-overlap? [a1 a2 b1 b2]
  (<= a2 b1))

(defn partial-overlap? [a1 a2 b1 b2]
  (<= a2 b2))

(defn proper-superset? [a1 a2 b1 b2]
  (> a2 b2))

(defn base-case? [a1 a2 b1 b2]
  (some nil? (list a1 a2 b1 b2)))

(defn head-partial [a1 a2 b1 b2]
  (list b1 a2))

(defn head-superset [a1 a2 b1 b2]
  (list b1 b2))

(declare swap-intervals)

(defn get-next-interval [a b]
  (let [[a1 a2] (first a)
        [b1 b2] (first b)]
    (cond (base-case? a1 a2 b1 b2)
            '()
          (no-overlap? a1 a2 b1 b2)
            (swap-intervals (rest a) b)
          (partial-overlap? a1 a2 b1 b2)
              (cons (head-partial a1 a2 b1 b2) (swap-intervals (rest a) b))
          (proper-superset? a1 a2 b1 b2)
            (cons (head-superset a1 a2 b1 b2) (swap-intervals a (rest b))))))

(defn swap-intervals [a b]
  (let [[a1 a2] (first a)
        [b1 b2] (first b)]
    (if (and a1 b1 (<= a1 b1))
        (get-next-interval a b)
        (get-next-interval b a))))

(defn intersect-intervals [a b]
  (if (or (== (count a) 0)
          (== (count b) 0))
    '()
    (let [a-sorted (sort-by #(first %) a)
          b-sorted (sort-by #(first %) b)]
      (swap-intervals a-sorted b-sorted))))


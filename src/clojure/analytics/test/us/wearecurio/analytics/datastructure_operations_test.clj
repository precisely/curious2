(ns us.wearecurio.analytics.datastructure-operations-test
  (:require [clojure.test :refer :all]
            [us.wearecurio.analytics.datastructure-operations :as dso]))

; Intersect lists of intervals.
(deftest intersect-intervals
  (testing "disjoint intervals"
    (is (= (dso/intersect-intervals '((0 10)) '((20 30)))
           '())))

  (testing "superset intervals"
    (is (= (dso/intersect-intervals '((0 10)) '((3 5)))
            '((3 5)))))

  (testing "partial overlap"
    (is (=  '((5 10))
           (dso/intersect-intervals '((0 10)) '((5 15))))))

  (testing  "3 regions of overlap"
    (is (= (dso/intersect-intervals '((0 10) (13 20)) '((5 15) (18 19.5)))
           '((5 10) (13 15) (18 19.5)))))

  (testing "intersecting with an empty list"
    (is (= (dso/intersect-intervals '() '((5 15) (18 19.5)))
          '()))))


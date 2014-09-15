(ns us.wearecurio.analytics.binify-test
  (:require [clojure.test :refer :all]
            [us.wearecurio.analytics.test-helpers :as th]
            [us.wearecurio.analytics.binify :as bi]
            [us.wearecurio.analytics.database :as db]
            [clj-time.coerce :as c]
            [clj-time.core :as t]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

;   Put data points into time bins of arbitrary size.
(deftest time-binning
  (testing "Case 0: 0 data points.  The time bins should be put into a hash even with 0 data points."
    (let [series []]
      (is (= (bi/binify series)
             {})))))

; Bin-generating functions.
(deftest binifying-one-data-point
  (testing "The reference point is midnight 1 Jan 1970 UTC."

    (let [series [{:tag_id 134
                   :date (db/sql-time 1970 1 1)
                   :data_type "CONTINUOUS"
                   :amount 4.000000000M
                   :id 47976}] ]

      (is (= (is (= (bi/binify series)
             {0 (list (first series))}))

      (is (= (bi/binify series bi/HOUR)
             {0 (list (first series))}))))))

  (testing "The default interval size is 1-day in ms (86400000)."
    (let [series [{:tag_id 134
                   :date (db/sql-time 1970 1 2)
                   :data_type "CONTINUOUS"
                   :amount 4.000000000M
                   :id 47976}]
          first-data-point (first series)]

      ; Passing in bi/DAY is not required.
      (is (= (bi/binify series)
             {1 (list first-data-point)}))

      (is (= (bi/binify series bi/HOUR)
             {24 (list first-data-point)})))))

(deftest binifying-two-data-points
  (testing "The default interval size is 1-day in ms (86400000)."
    (let [series [{:tag_id 134
                   :date (db/sql-time 1970 1 2)
                   :data_type "CONTINUOUS"
                   :amount 4.000000000M
                   :id 47976}

                  {:tag_id 134
                   :date (db/sql-time 1970 1 2 6 59)
                   :data_type "CONTINUOUS"
                   :amount 4.000000000M
                   :id 47976}]
          point-0 (first series)
          point-1 (second series)]

      ; Passing in bi/DAY makes the time-duration more explicit.
      (is (= (bi/binify series bi/DAY)
             {1 (list point-1 point-0)}))

      (is (= (bi/binify series bi/HOUR)
             {24 (list point-0)
              30 (list point-1)})))))

;
; count-points
;
;   count the number of points in each bin.
;
;
(deftest apply-transformation-to-time-bins
  (let [series [{:tag_id 134
                   :date (db/sql-time 1970 1 2)
                   :data_type "CONTINUOUS"
                   :amount 4.000000000M
                   :id 47976}

                  {:tag_id 134
                   :date (db/sql-time 1970 1 2 6 59)
                   :data_type "CONTINUOUS"
                   :amount 5.000000000M
                   :id 47976}]
          point-0 (first series)
          point-1 (second series)]

    (testing "Count the points in each bin."
      (is (= (bi/binify-by-count series bi/DAY)
             {1 2}))

      (is (= (bi/binify-by-count series bi/HOUR)
             {24 1
              30 1})))

    (testing "Take the max of the points"
      (is (= (bi/binify-by-max series bi/DAY)
             {1 5.000000000M}))

     (is (= (bi/binify-by-max series bi/HOUR)
            {24 4.000000000M
             30 5.000000000M})))

    (testing "Take the average of the points"
      (is (= (bi/binify-by-avg series bi/DAY)
             {1 4.500000000M}))

      (is (= (bi/binify-by-avg series bi/HOUR)
            {24 4.000000000M
             30 5.000000000M})))))

(deftest test-make-an-event-sequence
  (testing "Event-like series: Missing data should be zeroed out."
    ; We're assuming an input hash-map of data points.
    (let [start-date (bi/keyify (bi/date-time 2014 3 10))
          stop-date  (bi/keyify (bi/date-time 2014 3 15))
          series-data [{:id 4000 :user_id 99 :tag_id 2 :description "fun" :amount 6.750000000M
                 :date (db/sql-time 2014 3 12 23 0) :data_type "EVENT" }
                {:id 4010 :user_id 99 :tag_id 2 :description "fun" :amount 5.500000000M
                 :date (db/sql-time 2014 3 13 21 30 0) :data_type "EVENT"}
                {:id 4011 :user_id 99 :tag_id 2 :description "fun" :amount 7.333333000M
                 :date (db/sql-time 2014 3 14 23 30) :data_type "EVENT"}]
          averaged-bins (bi/binify-by-avg series-data)]
      (is (= (count series-data) 3) "Initial series should be length 3.")
      (is (= (count averaged-bins) 3) "Binned series should also be of length 3.")
      (is (= (count (bi/event-series averaged-bins start-date stop-date)) 6), "There are 5 days between Mar 10-15 including the last day, so the return value should be a sequence of length 6.")
      (testing "Test if it's equal up to 1 decimal place."
        (is (= (map (comp long (partial * 10)) (bi/event-series averaged-bins start-date stop-date))
               (map (comp long (partial * 10)) '(0 0 6.75 5.5 7.33 0)))))
      (testing "A start date after the first data point should exclude the first data point."
        (is (= (map (comp long (partial * 10)) (bi/event-series averaged-bins (bi/keyify (bi/date-time 2014 3 13)) stop-date ))
             (map (comp long (partial * 10)) '(5.5 7.33 0)))))

      )))

(deftest test-make-a-continuous-sequence
  (testing "Continuous series: Missing data should be interpolated between the most recent data ponts."
    (let [start-date (bi/keyify (bi/date-time 2014 3 10))
          stop-date  (bi/keyify (bi/date-time 2014 3 18))
          series-data [{:id 4000 :user_id 99 :tag_id 2 :description "fun" :amount 6.750000000M
                 :date (db/sql-time 2014 3 12 23 0) :data_type "EVENT" }
                {:id 4010 :user_id 99 :tag_id 2 :description "fun" :amount 5.500000000M
                 :date (db/sql-time 2014 3 15 21 30 0) :data_type "EVENT"}
                {:id 4011 :user_id 99 :tag_id 2 :description "fun" :amount 7.333333000M
                 :date (db/sql-time 2014 3 16 23 30) :data_type "EVENT"}]
          averaged-bins (bi/map-bins bi/avg-bin series-data)
          ordered-date-index-pairs (partition 2 1 (-> averaged-bins keys sort))
          last-data-point-y (bi/last-y averaged-bins)]
      (testing "Testing preconditions."
        (is (= (count series-data) 3) "Initial series should be length 3.")
        (is (= (count averaged-bins) 3) "Binned series should also be of length 3.")
        (is (= (count (bi/-continuous-series averaged-bins ordered-date-index-pairs start-date stop-date last-data-point-y)) 9), "There are 9 days between Mar 10-18 including the last day, so the return value should be a sequence of length 9."))
      (testing "Values before the first data point, should assume the value of the first data point."
        (is (= 675 (long (* 100 (first (bi/-continuous-series averaged-bins ordered-date-index-pairs start-date stop-date last-data-point-y))))))
        (is (= 675 (long (* 100 (second (bi/-continuous-series averaged-bins ordered-date-index-pairs start-date stop-date last-data-point-y)))))))
      (testing "Values after the last data point should assume the value of the last data point."
        (let [continuous-series (bi/-continuous-series averaged-bins ordered-date-index-pairs start-date stop-date last-data-point-y)
              last-pt (last continuous-series)
              second-to-last-pt (-> continuous-series reverse second)]
          (is (number? last-pt))
          (is (= 733 (long (* 100 last-pt))))
          (is (= 733 (long (* 100 second-to-last-pt))))))

      (testing "values between time-slices with data points should be interpolated."
        (let [continuous-series (bi/-continuous-series averaged-bins ordered-date-index-pairs start-date stop-date last-data-point-y)
              in-between-pt1 ((vec continuous-series) 3)
              in-between-pt2 ((vec continuous-series) 4)]
          (is (number? in-between-pt1))
          (is (= 633 (long (* 100 in-between-pt1))))
          (is (= 591 (long (* 100 in-between-pt2)))))))))


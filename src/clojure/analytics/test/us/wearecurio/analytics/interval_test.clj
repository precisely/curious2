(ns us.wearecurio.analytics.interval-test
  (:require [clojure.test :refer :all]
            [incanter.stats :as ist]
            [clj-time.core :as tc]
            [korma.core :as kc]
            [us.wearecurio.analytics.test-helpers :as th]
            [us.wearecurio.analytics.interval :as iv]
            [us.wearecurio.analytics.stats :as stats]
            [us.wearecurio.analytics.constants :as const]
            [us.wearecurio.analytics.database :as db]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

;
; -- Helpers for generating a fake data set in `data`
;

(defn sample-t-pois-helper [λ sum max-length]
  (let [Δt_next (ist/sample-exp 1 :rate λ)
        sum_next (+ sum Δt_next)]
    (if (> sum_next max-length)
      '()
      (cons sum_next (lazy-seq (sample-t-pois-helper λ sum_next max-length))))))

(defn sample-t-pois [λ max-length]
  (sample-t-pois-helper λ 0.0 max-length))

(defn pois-fn [λ]
  (fn [start-pos length]
    (apply sorted-set (map (partial + start-pos) (sample-t-pois λ length)))))

(def outside (pois-fn iv/λ_out))
(def inside  (pois-fn iv/λ_in))

(def data0 (clojure.set/union
             (outside 0 10)
             (inside 10 10)
             (outside 20 10)
             (inside 30 10)
             (outside 40 10)))

(def data1 (clojure.set/union
             (outside 0 10)
             (inside 10 10)
             (outside 20 10)
             (inside 30 10)
             (outside 40 10)))

(def data2 (clojure.set/union
             (inside 0 5)
             (outside 5 10)
             (inside 15 10)
             (outside 25 10)
             (inside 35 10)
             (outside 45 5)))

(def data3 (clojure.set/union
             (inside 0 5)
             (outside 5 10)
             (inside 15 10)
             (outside 25 10)
             (inside 35 10)
             (outside 45 5)))

; data is a hash-map of tag-id mapped to (sorted-set of entry date-times)
; *DEBUG*

(def data {42 data0
           99 data1
           100 data2
           101 data3 })

(def state (atom {}))

(def MAX-EPOCH 10)
(def alpha 0.0000001)

(defn set-state-to-known-values [state]
  (for [id '(100 99 101 42)
        x '(:in :out)]
    (swap! state assoc-in [:λ id x] (if (= :in x) 2 0.1))))

(deftest algo-7
  (testing "There should be 4 rows in the map"
    (is (= 4 (count data))))
  (testing "the initial state"
    (is (empty? @state))
    (iv/initialize-state data state 0 50)
    (set-state-to-known-values state)
    (is (clojure.set/superset? (-> @state keys set)
                               (set '(:C :λ :start-time :stop-time :phonebook))))))

; Test correlation computation.
;                                                   FILLED-IN
;                         tid=42    tid=99        tid=42   tid=99
;  DATE                    EVENT  CONTINUOUS      EVENT   CONTINUOUS
; 1/1/2014                   1
; ----- INTERVAL 1 START
; 1/2/2014                   1                      1       109*
; 1/3/2014                                          0*      109*
; 1/4/2014                           109            0*      109
; 1/5/2014                   1                      1       107*
; 1/6/2014                   1       105            1       105
; 1/7/2014                   1       110            1       110
; ----- INTERVAL 1 END
; 1/8/2014                           113
; 1/9/2014
; 1/10/2014
; 1/11/2014
; 1/12/2014                  1       109
; 1/13/2014
; 1/14/2014
; ----- INTERVAL 2 START
; 1/15/2014                  0                      0      103*
; 1/16/2014                          101            0*     101
; 1/17/2014                  1                      1      109*
; 1/18/2014                  1       117            1      117
; 1/19/2014                  1                      1      117*
; ----- INTERVAL 2 END
; 1/20/2014
; 1/21/2014
; 1/22/2014
;
;
; vectorized version: For tid=42, [  1   0     0   1   1   1   0   0   1   1   1]
; vectorized version: For tid=99, [108 108.5 109 107 105 110 103 101 109 117 118]
;
; normalized, tid=42, [
;             tid=99, [
;
; dot product,
; dot product divided by n-1
;

(deftest scenario-set-up
  (testing "selecting points within intervals for 1 tag series"
    (db/series-create 1 42 1 (th/sql-time 2014 1 1 18)  "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 2 18)  "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 5 18)  "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 6 18)  "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 7 18)  "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 12 18) "headache" "EVENT")
    (db/series-create 1 42 0 (th/sql-time 2014 1 15 18) "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 17 18) "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 18 18) "headache" "EVENT")
    (db/series-create 1 42 1 (th/sql-time 2014 1 19 18) "headache" "EVENT")

    ;(db/series-create 1 99 103 (th/sql-time 2014 1 2 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 109 (th/sql-time 2014 1 4 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 105 (th/sql-time 2014 1 6 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 110 (th/sql-time 2014 1 7 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 113 (th/sql-time 2014 1 8 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 109 (th/sql-time 2014 1 12 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 101 (th/sql-time 2014 1 16 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 117 (th/sql-time 2014 1 18 18) "temperature" "CONTINUOUS")
    ;(db/series-create 1 99 121 (th/sql-time 2014 1 22 18) "temperature" "CONTINUOUS")

    (let [cluster-run-id (db/cluster-run-create 1 :start-date (th/sql-time 2014 1 1)
                                                  :name "testing correlation computation"
                                                  :min-n 2 :interval-size-ms const/DAY)
          tag-cluster-id (db/tag-cluster-create cluster-run-id)]
      (db/tag-cluster-add-tag tag-cluster-id 42 1000)
      (db/tag-cluster-add-tag tag-cluster-id 99 2000)
      (db/cluster-interval-create tag-cluster-id (th/sql-time 2014 1 2) (th/sql-time 2014 1 7 23 59))
      (db/cluster-interval-create tag-cluster-id (th/sql-time 2014 1 15) (th/sql-time 2014 1 19 23 59))
      (testing "making vectors from tag series and intervals"
        (let [interval-1-start (iv/numeric-time const/DAY 2014 1 2)
              interval-1-stop  (iv/numeric-time const/DAY 2014 1 7 23 59)
              interval-2-start (iv/numeric-time const/DAY 2014 1 15)
              interval-2-stop  (iv/numeric-time const/DAY 2014 1 19 23 59)
              interval-pair-1  (list interval-1-start interval-1-stop)
              interval-pair-2  (list interval-2-start interval-2-stop)
              both-intervals   (list interval-pair-1 interval-pair-2) ]
          (is (= '(6 4 4 6 7 7)
                 (map #(long (* 10 %)) (iv/vectorize-in-intervals-cor 1 42 interval-1-start interval-1-stop (list interval-pair-1) :x))))
          (is (= '(4 4 6 7 6)
                 (map #(long (* 10 %)) (iv/vectorize-in-intervals-cor 1 42 interval-2-start interval-2-stop (list interval-pair-2) :x))))
          (is (= '(6 4 4 6 7 6 1 3 6 7 6)
                 (map #(long (* 10 %)) (iv/vectorize-in-intervals-cor 1 42 interval-1-start interval-2-stop both-intervals :x))))
          (is (= '(109 108 108 107 107 108)
                 (map long (iv/vectorize-in-intervals-cor 1 99 interval-1-start interval-1-stop (list interval-pair-1) :x))))
          (is (= '(1030 1052 1092 1134 1161)
                 (map  #(long (* 10 %)) (iv/vectorize-in-intervals-cor 1 99 interval-2-start interval-2-stop (list interval-pair-2) :x))))
          (is (= '(1090 1087 1081 1075 1078 1094 1041 1054 1092 1134 1161)
                 (map #(long (* 10 %)) (iv/vectorize-in-intervals-cor 1 99 interval-1-start interval-2-stop both-intervals :x))))
          (let [v1 (iv/vectorize-in-intervals-cor 1 42 interval-1-start interval-2-stop both-intervals :x)
                v2 (iv/vectorize-in-intervals-cor 1 99 interval-1-start interval-2-stop both-intervals :x)]

            ; In R,
            ;> cor(c(1.0, 0, 0, 1.0, 1.0, 1.0, 0.0, 0, 1.0, 1.0, 1.0), c(109.0, 109.0, 109.0, 107.0, 105.0, 110.0, 103.0, 101.0, 109.0, 117.0, 117.0))
            ;[1] 0.5136035

            (is (= 6541
                   (-> (stats/cor v1 v2) (* 10000) long)))))))))

(ns us.wearecurio.analytics.interval-test
  (:require [clojure.test :refer :all]
            [incanter.stats :as ist]
            [us.wearecurio.analytics.test-helpers :as th]
            [us.wearecurio.analytics.interval :as iv]
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


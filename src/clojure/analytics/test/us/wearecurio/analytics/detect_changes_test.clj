(ns us.wearecurio.analytics.detect-changes-test
  (:require [clojure.test :refer :all]
            [us.wearecurio.analytics.detect-changes :as dc] 
            [us.wearecurio.analytics.stats :as stats] 
            [us.wearecurio.analytics.test-helpers :as th]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

; The test sequence looks like this:
; [0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 100 100 100 100 100 100 100 100 100 100]
; So the change points happen every 10 steps.
(deftest get-indices-of-change-points
  (is (= [9 19 29 39]
      (into [] (dc/change-point-indexes (stats/step-function 10 0 10 1 10 -1 10 2 10 100))))))

(deftest get-indices-of-change-points
  (is (= [9 19 29 39]
      (into [] (dc/change-point-indexes (stats/step-function 10 0 10 1 10 -1 10 2 10 100))))))


(ns us.wearecurio.analytics.stats-test
  (:require [clojure.test :refer :all]
            [us.wearecurio.analytics.datastructure-operations :as dso] 
            [us.wearecurio.analytics.stats :as stats] 
            [us.wearecurio.analytics.test-helpers :as th]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

(deftest smoothing-function
  (let [s0 (stats/step-function 10 0 10 1)]
    (is true)))

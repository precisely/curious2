(ns us.wearecurio.analytics.idioms-test
  (:require [clojure.test :refer :all]
            [us.wearecurio.analytics.test-helpers :as th]
            [us.wearecurio.analytics.idioms :as im]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

(deftest idiom-test
  (testing "dissoc-in"
    (let [m {:a {:apple [100 200 300]}
             :b {:banana [ 3 4 5]}}]  
      (is (= (im/dissoc-in m [:a :apple])
             {:a {}
              :b {:banana [3 4 5]}}))))
  (testing "apply-to-values"
    (is (= (im/apply-to-values #(* % %) {:a 1 :b 2 :c 3})
           {:a 1 :b 4 :c 9})))
  (testing "collect-into-index"
    (let [col [{:name "David" :shoe-size 9}
               {:name "Roger" :shoe-size 9}
               {:name "Love" :shoe-size 2}]]
      (is (= (->> (im/collect-into-index col :shoe-size :name)
                  (im/apply-to-values set))
             {9 (set '("David" "Roger"))
              2 (set '("Love"))})))))


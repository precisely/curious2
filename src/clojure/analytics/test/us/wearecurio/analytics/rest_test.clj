(ns us.wearecurio.analytics.rest-test
  (:require [clojure.test :refer :all]
            [incanter.stats :as ist]
            [clj-time.core :as tc]
            [korma.core :as kc]
            [org.httpkit.fake :as hkf]
            [us.wearecurio.analytics.test-helpers :as th]
            [us.wearecurio.analytics.rest :as re]
            [us.wearecurio.analytics.constants :as const]
            [us.wearecurio.analytics.database :as db]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)
(def TICK 1)
(def TIMEOUT 2000)
(def waited (atom 0))
(def ran-test (atom false))

(defmacro test-when [the-cond & body]
  "Poll/wait until a condition happens, then run the test."
  `(do
    (reset! waited 0)
    (reset! ran-test false)
    (while (and (not @ran-test) (< @waited TIMEOUT))
      (when ~the-cond
        (do
          (reset! ran-test true)
          ~@body))
      (Thread/sleep TICK)
      (swap! waited (partial + TICK)))
    ; Run the test if it timed out.
    (when (not @ran-test) (do ~@body))))


;
;
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
; 1/21/2014                          121
; 1/22/2014                          120
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

(deftest worker-state-transtitions
  (testing "state transitions of worker process as it goes from clustering to computing correlation"
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

    (db/series-create 1 99 103 (th/sql-time 2014 1 2 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 109 (th/sql-time 2014 1 4 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 105 (th/sql-time 2014 1 6 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 110 (th/sql-time 2014 1 7 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 113 (th/sql-time 2014 1 8 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 109 (th/sql-time 2014 1 12 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 101 (th/sql-time 2014 1 16 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 117 (th/sql-time 2014 1 18 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 121 (th/sql-time 2014 1 21 18) "temperature" "CONTINUOUS")
    (db/series-create 1 99 120 (th/sql-time 2014 1 22 19) "temperature" "CONTINUOUS")

    (re/start-server)

    (testing "starting the server initializes the worker state"
      (is (= (re/get-status) re/READY)))

      (let [task-id 500]
  
        (testing "normal completion of one-off jobs"
          (re/init-run-state)
          (is (= re/READY (re/get-status)))
          (is (= re/NEW (re/get-sub-status)))
  
          ; Make sure the function that updates the status works.
          (re/update-state! re/RUNNING  re/CLUSTERING)
          (is (= re/CLUSTERING (re/get-sub-status)))
  
          ; Run the clustering job.
          (re/start-clustering-job-handler 1 {:params {:task-id task-id
                                                       :silent true
                                                       }})
  
          ;; Wait for clustering thread to start clustering.
          (test-when (= re/RUNNING (re/get-status))
                     (is (= re/CLUSTERING (re/get-sub-status))))
  
          ; Wait for clustering thread to start computing correlations.
          (test-when (= re/CORRELATING (re/get-sub-status))
                     (is true "The CORRELATING sub-status was reached."))
  
          ; Wait for clustering thread to stop normally.
          (test-when (= re/READY (re/get-status))
                     (is (= re/COMPLETED (re/get-sub-status)))))

        (testing "normal completion of child tasks"
          ; Run the clustering job.
          (re/start-clustering-job-handler 1 {:params {:task-id task-id
                                                       :task-type "collection-child"
                                                       :silent true}})
  
          (test-when (= re/READY (re/get-status))
                     (or (is (= re/COMPLETED-ALL (re/get-sub-status))
                             (= re/COMPLETED-ALL (re/get-sub-status)))))

          (test-when (= re/READY (re/get-status))
                     (is (= nil (re/get-error)))))

        (testing "error handling"
          ; Run the clustering job.
          (re/start-clustering-job-handler 1 {:params {:task-id task-id
                                                       :task-type "collection-child"
                                                       :silent true}
                                              :test-throw true})

          (test-when (= re/ERROR (re/get-sub-status))
                     (do (is (= re/ERROR (re/get-sub-status)))
                         (is (not (= nil (re/get-error))))
                         ; There should be a fairly long error message stored in the :error field.
                         (is (< 200 (count (re/get-error))))))

          (testing "We should reach the request next state even if there is an error. "
            (test-when (= re/REQUEST-NEXT (re/get-sub-status))
                       (is (= re/REQUEST-NEXT (re/get-sub-status))))))

        (testing "when there are no more users to process."
          (hkf/with-fake-http [{:url (str re/WEB-URL re/WEB-NEXT-PATH) :method :post} {:status 200 :body "{\"userId\": -1}"}]
             (re/after-cluster "collection-child" task-id)
             (test-when (= re/COMPLETED-ALL (re/get-sub-status))
                        (is (= re/COMPLETED-ALL (re/get-sub-status)))
                        (is (= re/READY (re/get-status))))))

          )))



(ns us.wearecurio.analytics.database-test
  (:require [clojure.test :refer :all]
            [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.test-helpers :as th]
            [korma.db :as kd]
            [korma.core :as kc]
            [clj-time.coerce :as c]
            [clj-time.core :as t]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

; For a given user, list elements of all series within
;   a date range.
(deftest test-series-range

  (testing ""
    (let [user1-id      1
          user2-id      2
          tag1-id       2000
          tag2-id       3000
          tag3-id       4000
          description1  "eggs"
          description2  "bacon"
          description3  "ham"
          date1        (db/sql-time 2011 1 1)
          date2        (db/sql-time 2011 1 2)
          date3        (db/sql-time 2011 1 3)
          date4        (db/sql-time 2011 1 4)
          date5        (db/sql-time 2011 1 5)]

      (testing "inital state of input table analytics_time_series"
        (is (= 0 (db/series-count))))

      (do
        ; Create some entries.
        ; User 1 is tracking tags 1 and 2.
        (db/series-create user1-id tag1-id 1 date1 description1)
        (db/series-create user1-id tag1-id 1 date2 description1)
        (db/series-create user1-id tag1-id 1 date3 description1)

        (db/series-create user1-id tag2-id 2 date1 description2)
        (db/series-create user1-id tag2-id 3 date2 description2)
        (db/series-create user1-id tag2-id 4 date3 description2)

        ; User 2 is tracking tags 2 and 3 on the same dates plus an extra day.
        (db/series-create user2-id tag1-id 99 date4 description2)

        (db/series-create user2-id tag2-id 100 date1 description2)
        (db/series-create user2-id tag2-id 200 date2 description2)
        (db/series-create user2-id tag2-id 300 date3 description2)
        (db/series-create user2-id tag2-id 300 date4 description2)

        (db/series-create user2-id tag3-id 0  date1 description3)
        (db/series-create user2-id tag3-id 7  date2 description3)
        (db/series-create user2-id tag3-id 14 date3 description3)
        (db/series-create user2-id tag3-id 21 date4 description3)

        ; There should be 15 entries in analytics time-series.
        (is (= 15 (db/series-count)))

        ; Selecting all of user 1's data should retrieve 6 elements.
        (is (= 6 (count (db/series-range date1 date5 user1-id))))

        ;; Selecting all of user 2's data should retrieve 8 elements.
        (is (= 9 (count (db/series-range date1 date5 user2-id))))

        ; Selecting all the data from 1970-01-01 to 1970-01-03,
        ;  should not include the end points, so user 1 and user 2
        ;  should return 4 elements each.

        (is (= 4 (count (db/series-range date1 date3 user1-id))))
        (is (= 4 (count (db/series-range date1 date3 user2-id))))

        ; The actual amounts for each user should be:
        (is (= '(1 1 2 3)
               (map (comp long :amount) (db/series-range date1 date3 user1-id))))

        (testing "Select a distinct list of tag ids that have data strictly before a certain date."
          ; Should not include the date if it's well before the given date(-time).
          (is (= (set (list 3000 4000))
                 (set (db/list-tag-ids-before-date date3 user2-id))))

          ; Should not include the date if it is exactly at the same time as the given date.
          (is (= (set (list 3000 4000))
                 (set (db/list-tag-ids-before-date date4 user2-id))))

          ; Should include it if it well before the given date.
          (is (= (set (list 2000 3000 4000))
                 (set (db/list-tag-ids-before-date date5 user2-id)))))))))

(def UID 4); an arbitrary user id.

(deftest cluster-run-crud
  (testing "cluster-run table should be empty"
    (println (db/cluster-run-list))
    (is (= (count (db/cluster-run-list)) 0)))
  (testing "cluster-run-create: should be able to insert a row."
    (let [start-date (db/sql-time 2014 1 1)
          stop-date (db/sql-time 2014 2 2)]
        (db/cluster-run-create UID)
        (is (= (count (db/cluster-run-list)) 1))))
  (testing "cluster-run-create: start-date and stop-date should be able to be specified."
        (db/cluster-run-create UID :start-date db/BIG-BANG :stop-date (db/sql-time 2014 2 14))
        (is (= (count (db/cluster-run-list)) 2))
        (let [last-run (-> (db/cluster-run-list) last)]
          (is (= (:start_date last-run) db/BIG-BANG))
          (is (= (:stop_date last-run) (db/sql-time 2014 2 14)))
          (testing "default interval size is one day in milliseconds."
            (is (= (:interval_size_ms last-run 86400000))))))
  (testing "cluster-run-create: min-n, interval-size (time-scale e.g. # of obs per unit time) and name shoud be able to be specified."
        (db/cluster-run-create UID :name "clustering is fun" :min-n 42 :interval-size-ms 5000)
        (is (= (count (db/cluster-run-list)) 3))
        (let [last-run (-> (db/cluster-run-list) last)]
          (is (= (:min_n last-run 42)))
          (is (= (:name last-run "clustering is fun")))
          (is (= (:interval_size_ms last-run 5000)))))
  (testing "cluster-run-update-finished: time stamp indicating job is finished."
    (is (= (count (db/cluster-run-list)) 3))
    (let [last-run (db/cluster-run-latest UID)]
      (is (nil? (:finished last-run)))
      (db/cluster-run-update-finished (:id last-run))
      (is (= java.sql.Timestamp (class (:finished (db/cluster-run-latest UID)))))))
  )

(deftest tag-cluster-crud
  (testing "tag-cluster initial state: cluster-run and tag-cluster tables should be empty to start with"
    (println (db/cluster-run-list))
    (is (= (count (db/cluster-run-list)) 0))
    (is (= (count (db/tag-cluster-list)) 0)))
  (testing "tag-cluster: should be able to insert a row."
    (let [last-run-id          (db/cluster-run-create 4)
          last-tag-cluster-id  (db/tag-cluster-create last-run-id)]
      (is (= (count (db/cluster-run-list)) 1))
      (is (= (count (db/tag-cluster-list)) 1))
      (testing "tag-cluster: add tag-ids to the tag cluster."
        (let [tag-ids (fn [] (db/tag-cluster-list-tag-ids last-tag-cluster-id))]
          (is (= '() (tag-ids)))
          (db/tag-cluster-add-tag last-tag-cluster-id 42 1000)
          (db/tag-cluster-add-tag last-tag-cluster-id 99 2000)
          (db/tag-cluster-add-tag last-tag-cluster-id 100 3000)
          (is (contains? (set (tag-ids)) 42))
          (is (contains? (set (tag-ids)) 99))
          (is (contains? (set (tag-ids)) 100))
          (is (not (contains? (set (tag-ids)) 41)))
          (testing "tag-cluster-tag: loglikelihood should be saved"
            (is (clojure.set/superset?
                  (set (map #(long (:loglike %))
                            (:analytics_tag_cluster_tag (first (db/tag-cluster-get last-tag-cluster-id)))))
                  (set '(1000 2000 3000)))))
          (testing "tag-cluster: delete all tag-ids from the tag cluster."
            (is (= 3 (count (tag-ids))))
            (db/tag-cluster-tag-delete last-tag-cluster-id)
            (is (= 0 (count (tag-ids))))))))))

(deftest cluster-run-crud
  (testing "cluster-run initial state: all tables should be empty."
    (is (= (count (db/cluster-run-list)) 0))
    (is (= (count (db/tag-cluster-list)) 0))
    (is (= (count (db/tag-cluster-tag-list)) 0))
    (is (= (count (db/cluster-interval-list)) 0)))
  (testing "should be able to insert a row into a cluster-interval"
    (let [run-id          (db/cluster-run-create UID)
          tag-cluster-id  (db/tag-cluster-create run-id)
          interval1-id    (db/cluster-interval-create tag-cluster-id (db/sql-time 2014 1 2) (db/sql-time 2014 3 4))
          interval2-id    (db/cluster-interval-create tag-cluster-id (db/sql-time 2014 5 6) (db/sql-time 2014 7 8))]
      (is (= (count (db/cluster-run-list)) 1))
      (is (= (count (db/tag-cluster-list)) 1))
      (is (= (count (db/tag-cluster-tag-list)) 0))
      (is (= (count (db/cluster-interval-list)) 2))
      (is (= (:analytics_tag_cluster_id (last (db/cluster-interval-list-by-tag-cluster-id tag-cluster-id)))
             tag-cluster-id))
      (is (= (:start_date (last (db/cluster-interval-list-by-tag-cluster-id tag-cluster-id)))
             (db/sql-time 2014 1 2)))
      (is (= (:stop_date (last (db/cluster-interval-list-by-tag-cluster-id tag-cluster-id)))
             (db/sql-time 2014 3 4)))
      (db/cluster-interval-delete-by-tag-cluster-id tag-cluster-id)
      (is (= (count (db/tag-cluster-list)) 1))
      (is (= (count (db/cluster-interval-list)) 0)))))

(deftest cascading-delete
  (testing "tag-cluster-tags (tag-ids associated with a tag-cluster) should all be deleted."
    (is (= (count (db/cluster-run-list)) 0))
    (is (= (count (db/tag-cluster-list)) 0))
    (is (= (count (db/tag-cluster-tag-list)) 0))
    (is (= (count (db/cluster-interval-list)) 0))
    ; Set up 1 cluster run and attach 3 tag clusters each with 5 tag-ids
    (let [run-id (db/cluster-run-create UID)
          wrong-run-id (inc run-id)]
      (dotimes [i 3]
        (let [tag-cluster-id (db/tag-cluster-create run-id)]
          (dotimes [j 5]
            (db/tag-cluster-add-tag tag-cluster-id (* (inc i) (inc j)) 5000)
            (db/cluster-interval-create tag-cluster-id
                                        (db/sql-time 2014 (inc j) (inc i))
                                        (db/sql-time 2014 (inc j) (inc (inc i)))))))
      (is (= (count (db/cluster-run-list)) 1))
      (is (= (count (db/tag-cluster-list)) 3))
      (is (= (count (db/tag-cluster-tag-list)) 15))
      (is (= (count (db/cluster-interval-list)) 15))
      (db/cluster-run-delete wrong-run-id)
      (is (= (count (db/cluster-run-list)) 1))
      (is (= (count (db/tag-cluster-list)) 3))
      (is (= (count (db/tag-cluster-tag-list)) 15))
      (is (= (count (db/cluster-interval-list)) 15))
      (db/cluster-run-delete run-id)
      (is (= (count (db/cluster-run-list)) 0))
      (is (= (count (db/tag-cluster-list)) 0))
      (is (= (count (db/tag-cluster-tag-list)) 0))
      (is (= (count (db/cluster-interval-list)) 0)))))



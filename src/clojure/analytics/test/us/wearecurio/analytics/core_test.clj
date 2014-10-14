(ns us.wearecurio.analytics.core-test
	(:require [us.wearecurio.analytics.database :as db]
						[us.wearecurio.analytics.test-helpers :as th]
						[us.wearecurio.analytics.core :refer :all]
						[korma.db :as kd]
						[korma.core :as kc]
						[midje.sweet :refer :all]
						[clj-time.coerce :as c]
						[clj-time.core :as t]))

(th/before-all)

; List elements of the series.
(facts "LIST data points of series."
	(fact "The table should be empty to start with."
		(let [user_id 1
					tag_id 1]
			(count (db/series-list user_id tag_id)) => 0)))

; Insert element into series.
(facts "CREATE data point in series."
	(let [user_id 1
			 tag_id 2
			 amount 3.5
			 date (db/sql-time 1986 10 14)
			 description "tag 1"]
	 (do
		; Create two entries.
		(db/series-create user_id tag_id (dec amount) date "tag foo")
		(db/series-create user_id tag_id amount date description)

		(let [series (db/series-list user_id tag_id)
					first-entry (first series)
					last-entry (last series)]
			(fact "first entry is 'tag foo'"
				 (:description first-entry) => "tag foo")
			(fact "first entry amount is 2.5"
				 (== (:amount first-entry) 2.5)=> true)
			(fact "amount == 3.5"
				 (== (:amount last-entry) 3.5) => true)
			(fact "description = 'tag 1'"
				(:description last-entry) => "tag 1")
			(fact "date should be 1986-10-14"
				(c/from-sql-time (:date last-entry)) => (t/date-time 1986 10 14))))))
(th/erase-tables)

; List users
(facts "List the distinct user_ids."
	(doseq [x [1 2 3]]
		(let [user-id-1 1
					user-id-2 2
					tag_id (* 5 x)
					date (db/sql-time 1986 10 x)]
			(db/series-create user-id-1 (* 5 x) 42 date "tag 1")
			(db/series-create user-id-1 (* 5 x) 13 date "tag 1")
			(db/series-create user-id-2 (* 5 x) 7 date "tag 1")))

		(fact "There are 9 entries total."
			(db/series-count) => 9)
		(fact "There are 3 unique tag."
			(db/list-tag-ids 1) => [5 10 15])
		(fact "There are 2 users."
			(db/list-user-ids) => [1 2])
		(fact "There should be no entries after deleting the series."
			(db/series-delete 1 5)
			(db/series-delete 1 10)
			(db/series-delete 1 15)
			(db/series-count) => 3))
(th/erase-tables)

(facts "Collect values into days and average them."
	(doseq [x [1 2 3]]
		(let [user-id-1 1
					user-id-2 2
					tag_id (* 5 x)
					amount (* 10 x)
					descr (str "tag " tag_id)
					date (db/sql-time 1986 10 x)]
			(db/series-create user-id-1 tag_id amount				date descr)
			(db/series-create user-id-1 tag_id (* 2 amount)	date descr)
			(db/series-create user-id-2 tag_id (* 10 amount) date descr)))
			; user tag amount date
			; 1		 5	 10			1986-10-1
			; 1		 5	 20			1986-10-1
			; 1		 10  20			1986-10-2
			; 1		 10  40			1986-10-2
			; 1		 15  30			1986-10-3
			; 1		 15  60			1986-10-3

			; 2		 5	 100		1986-10-1
			; 2		 10  200		1986-10-2
			; 2		 15  300		1986-10-3
			(fact "the first user should have the values collected into a hash"
				(-> (day-values (db/series-list 1 5)) class)
					=> clojure.lang.PersistentArrayMap)
			(fact "the keys should be a unique collection formatted days."
				(-> (db/series-list 1) day-values keys set)
					=> (set '("1986-10-01" "1986-10-02" "1986-10-03"))) ;'
			(fact "the average of they first day should be 15"
				(-> (db/series-list 1) day-values (get "1986-10-01") avg int)
					=> 15))

(facts "Average avg()"
	(fact "The avg() of an empty list is nil."
		(avg (list))
			=> nil)
	(fact "The avg() of a number is the number itself."
		(avg 300)
			=> 300)
	(fact "The avg() of a 1 and 2 is 1.5"
		(-> (avg [1 2]) (* 10) int)
			=> 15)
	(fact "The avg() of a row of the same numbers is the number itself."
		(-> (avg (list 42 42 42 42 42)) (* 10) int)
			=> 420)
	(fact "The avg() of nil is nil"
		(avg nil)
			=> nil)
	(fact "The avg() of [12, 7, 4, 29, 16] is 13.6"
		(-> (avg [12 7 4 29 16]) (* 10) int)
			=> 136))

(facts "Standard deviation sd()."
	(fact "The sd() of the empty list is nil."
		(sd (set (list)))
			=> nil)
	(fact "The sd() of the empty vector is nil."
		(sd '())
			=> nil)
	(fact "The sd() of the empty set is nil."
		(sd  [])
			=> nil)
	(fact "The sd() of a single number is 1 by definition."
		(sd 42)
			=> 0
		(sd '(42))
			=> 0
		(sd '(0))
			=> 0)
	(fact "The sd() of 1 and 2 is 0.707."
		(-> (sd '(1 2)) (* 1000) int)
			=> 707)
	(fact "The sd() of '(1 2 3) is 1"
		(-> (sd (list 1 2 3)) (* 1000) int)
			=> 1000)
	(fact "The sd() of [42, 55, 99] is 29.87"
		(-> (sd [42 55 99]) (* 100) int)
			=> 2987)
	(fact "The sd() of [42 42 42 42 42] is 0"
		(sd [42 42 42 42 42])
			=> 0.0))

(th/erase-tables)
(facts "Get daily average amounts from a series."
	(doseq [x [1 2 3]
					y [4 5 6]]
		(let [user-id 1
					tag_id (* 5 x)
					amount (* 10 x y)
					descr (str "tag " tag_id)
					date (db/sql-time 1986 10 y)]
			;(println (str user-id " " tag_id	" " amount " " date))
			(db/series-create user-id tag_id amount date descr)
			(db/series-create user-id tag_id (inc amount) date descr)))

				; user tag amount date
				; 1		 5	 40			1986-10-03
				; 1		 5	 41			1986-10-03
				; 1		 5	 50			1986-10-04
				; 1		 5	 51			1986-10-04
				; 1		 5	 60			1986-10-05
				; 1		 5	 61			1986-10-05
				;
				; 1		 10  80			1986-10-03
				; 1		 10  81			1986-10-03
				; 1		 10  100		1986-10-04
				; 1		 10  101		1986-10-04
				; 1		 10  120		1986-10-05
				; 1		 10  121		1986-10-05
				;
				; 1		 15  120		1986-10-03
				; 1		 15  121		1986-10-03
				; 1		 15  150		1986-10-04
				; 1		 15  151		1986-10-04
				; 1		 15  180		1986-10-05
				; 1		 15  181		1986-10-05

			(fact "The averages user 1 tag 5 should be '(40.5 50.5 60.5)"
				(set (vals (series-daily-average 1 5)))
					=> (set '(40.5 50.5 60.5))))
(th/erase-tables)


(facts "Inner product between two datetime-value maps"
	(let [series1 {:a 1 :b 2 "c" 3}
				series2 {:a 3 :b 5 "c" 7}]
		(fact "Should return a vector of products"
			(set (inner-product series1 series2)) => (set '(3 10 21)))
		(fact "If there is a place with no overlap it shouldn't affect the answer."
			(set (inner-product  (assoc series1 :d 42) series2)) => (set '(3 10 21))
			(set (inner-product  series2 (assoc series1 :d 42))) => (set '(3 10 21)))))

(facts "Count days in common."
	(let [series1 {"2014-01-01" 19 "2014-01-02" 34 "2014-01-03" 38 "2014-01-04" 32}
				series2 {"2014-01-02" 83 "2014-01-04" 77 "2014-01-22" 54}]
		(fact "Overlaping days are: '2014-01-02' and '2014-01-04'."
			(set (overlapping-keys series1 series2)) => (set '("2014-01-02" "2014-01-04")))
		(fact "Number of overlapping elements is 2."
			(num-overlap series1 series2) => 2)))

(th/erase-tables)
(facts "Saving scores."
	(let [s1 {:user-id 1 :tag1-id 2 :tag2-id 3	:score 42 :overlap-n 5 :tag1-type "tag" :tag2-type "tag"}
				s2 {:user-id 1 :tag1-id 2 :tag2-id 3	:score 43 :overlap-n 5 :tag1-type "tag" :tag2-type "tag"}
				s3 {:user-id 1 :tag1-id 2 :tag2-id 3	:score 44 :overlap-n 5 :tag1-type "tag" :tag2-type "tag"}
				s4 {:user-id 1 :tag1-id 2 :tag2-id 42 :score 50 :overlap-n 5 :tag1-type "tag" :tag2-type "tag"}
				s5 {:user-id 1 :tag1-id 2 :tag2-id 3	:score 44 :overlap-n 5 :tag1-type "tag" :tag2-type "tag"}
				s6 {:user-id 2 :tag1-id 2 :tag2-id 3	:score 45 :overlap-n 5 :tag1-type "tag" :tag2-type "tag"}]
		(fact "The correlation table should start off empty."
			(db/score-count)
				=> 0
			(count (db/score-find 1 2 3 "tag" "tag"))
				=> 0)
		(fact "After saving score1, we should be able to find it again."
			(do (db/score-update-or-create (s1 :user-id)
									(s1 :tag1-id)
									(s1 :tag2-id)
									(s1 :score)
                  (s1 :overlap-n)
									(s1 :tag1-type)
									(s1 :tag2-type))
				 (db/score-count)
					 => 1
				 (db/score-count (s1 :user-id))
					 => 1
				 (db/score-count (s1 :user-id) (s1 :tag1-id) (s1 :tag2-id))
					 => 1
				 (db/score-count 500)
					 => 0
				 (db/score-count (s1 :user-id) (s1 :tag1-id) (-> (s1 :tag2-id) inc))
					 => 0))
		 (fact "Saving should be idempotent."
			 (do
					 (db/score-update-or-create (s1 :user-id)
											 (s1 :tag1-id)
											 (s1 :tag2-id)
											 (s1 :score)
                       (s1 :overlap-n)
											 (s1 :tag1-type)
											 (s1 :tag2-type))

					 (db/score-update-or-create (s1 :user-id)
											 (s1 :tag1-id)
											 (s1 :tag2-id)
											 (s1 :score)
                       (s1 :overlap-n)
											 (s1 :tag1-type)
											 (s1 :tag2-type))
				 (db/score-count)
					 => 1
				 (db/score-count (s1 :user-id))
					 => 1))))

(th/erase-tables)

(th/after-all)

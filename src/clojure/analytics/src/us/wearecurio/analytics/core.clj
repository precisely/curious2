(ns us.wearecurio.analytics.core
	(:require [us.wearecurio.analytics.database :as db]
						[clj-time.coerce :as c]
						[clj-time.format :as f]
						[clojure.math.numeric-tower :as math]))

; NB: To use most functions in this namespace you must first call
;  (connect).
(defn connect []
	"A convenience wrapper for use at the REPL."
	(db/connect))

(def day-formatter (f/formatters :year-month-day))

(defn to-day-string [entry]
	(f/unparse day-formatter (c/from-sql-time (:date entry))))

(defn as-day-string [entry]
	(assoc entry :date (to-day-string entry)))

(defn format-day-values [series]
	(map as-day-string series))

(defn collect-into-index [col index-key value-key]
	"Create an value-based index from a seq of maps.	index-key is the key whose values will be used to collect values of value-key."
	; E.g.
	; (def the-collection [
	;		 {:name "David" :shoe-size 9}
	;		 {:name "Roger" :shoe-size 9}
	;		 {:name "Love"	:shoe-size 3}])
	;
	; (collect-into-index the-collection :shoe-size :name)
	;		=> {9 ("David" "Roger")
	;				3 ("Love")}
	;
	(reduce (fn [accum nex]
						(let [k (get nex index-key)
									v (get nex value-key)
									collected (get accum k)]
							(assoc accum k (conj collected v))))
					{} col))

(defn day-values [series]
	(collect-into-index (format-day-values series) :date :amount))

(defn avg [v]
	"The average of a vector of values."
	(cond
		(nil?		 v) nil
		(number? v) v
		(and (coll? v) (= 0 (count v))) nil
	:else
		(let [n		(* 1.0 (count v))
					sum (reduce + v)]
			(/ sum n))))

(defn sd [v]
	"The standard deviation of a bunch of numbers."
	(cond
		(number? v)
			0
		(and (coll? v) (= (count v) 0))
			nil
		(and (coll? v) (= (count v) 1))
			0
		(and (coll? v) (> (count v) 1))
			(let [mu (avg v)
						n (dec (count v))
						v2 (map (fn [x] (* (- x mu) (- x mu))) v)
						sum2 (reduce + v2)]
				(math/sqrt (/ sum2 n)))))

(defn apply-to-values [f hash-map]
	(into {} (for [[k v] hash-map] [k (f v)])))

(defn series-daily-average [& args]
	"Get a sequence of [{\"YYYY-mm-dd\" <AMOUNT>} ...] where each entry represents the average amount for the day. E.g.
	{\"1986-10-04\" 40.5, \"1986-10-05\" 50.5, \"1986-10-06\" 60.5}"
	(let [series (day-values (apply db/series-list args))]
		(apply-to-values avg series)))

(defn standardize [x mu s]
	"Take a value and subtract the mean and divide by the standard deviation."
	(/ (- x mu) s))

(defn replace-values-with-ones [hmap]
	"Replace a hash-map's values with the floating point number 1."
	(apply-to-values (fn [x] 1.0) hmap))

(defn standardize-series [user-id tag-id]
	"Return the standardized series of a sequence of tags."
	; When-let essentially checks for nil.
	(when-let [series (series-daily-average user-id tag-id)]
		; Compute the size of the series once ahead of time.
		(let [size (count series)]
			; Return the empty series if it's empty.
			(if (= 0 size)
				series
				(let [v  (vals series)
							mu (avg v)
							s  (sd v)]
					; If the standard deviation is 0, the series has all
					;		the same values, so it's essentialy a stream of
					;		events.  So standardize events to the value of 1.
					(if (== s 0)
						(replace-values-with-ones series)
						(apply-to-values #(standardize % mu s) series)))))))

(defn inner-product [hmap1 hmap2]
	"Given a hash-map of (datetime-as-key, amount-as-value) entries, return the product of the values."
	(letfn [(prod [[k1 v1]]
						 (if-let [v2 (get hmap2 k1)]
							 (* v1 v2)))]
		(keep prod hmap1)))

(defn overlapping-keys [hmap1 hmap2]
	"Given two hashmaps, count the number of intersecting keys"
	(let [s1 (-> hmap1 keys set)
				s2 (-> hmap2 keys set)]
		(clojure.set/intersection s1 s2)))

(defn num-overlap [hmap1 hmap2]
	"Given two time-series hashes, count the number of intersecting days/time points."
	(count (overlapping-keys hmap1 hmap2)))

(defn compute-mipss [user-id tag1-id tag2-id]
	"Average product of overlapping standardized day values."
	(let [ser1	(standardize-series user-id tag1-id)
				ser2	(standardize-series user-id tag2-id)
				mipss (avg (inner-product ser1 ser2))
				n			(num-overlap ser1 ser2)]
		(when mipss
			{:mipss mipss :n n})))

(defn print-mipss [user-id tag1-id tag2-id score]
	(println user-id " " tag1-id " " tag2-id " " (score :mipss) " " (score :n)))

(defn compute-and-save-score [user-id tag1-id tag2-id]
	(when-let [mipss					(compute-mipss user-id tag1-id tag2-id)]
		(let [score					(mipss :mipss)
          overlap-n     (mipss :n)]
			;(print-mipss user-id tag1-id tag2-id mipss)
			(db/score-update-or-create user-id tag1-id tag2-id score overlap-n "tag" "tag"))))

(defn update-all-users []
	"For each user in anaytics_time_series, iterate over all that user's tag-pairs, compute the MIPSS, then save it to the correlation table."
	(db/for-all-users-and-tag-pairs compute-and-save-score))

(defn update-user [user-id]
	(db/for-all-tag-pairs (partial compute-and-save-score user-id) user-id))

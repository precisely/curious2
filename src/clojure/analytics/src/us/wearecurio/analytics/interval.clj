(ns us.wearecurio.analytics.interval
  (:require [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.idioms :as im]
            [us.wearecurio.analytics.binify :as bi]
            [us.wearecurio.analytics.constants :as const]
            [us.wearecurio.analytics.datastructure-operations :as dso]
            [us.wearecurio.analytics.detect-changes :as dc]
            [us.wearecurio.analytics.stats :as stats]
            [clj-time.core :as tc]
            [clj-time.coerce :as tr]
            [clj-time.format :as tf]
            [incanter.stats :as ist]
            [incanter.core :as ic]
            [clojure.math.numeric-tower :as math]))

; Tuning parameter α for DP.
; Number of clusters is proportional to α∙logN.

;(db/connect (db/connection-params "DEVELOPMENT"))

(def state (atom {})) ; The state of each iteration.
(def α 0.00001) ; The main Dirichlet Process hyper parameter.
(def λ_in 2)
(def λ_out 0.1)
(def MAX-ITER-NEW-CLUSTER 2000)
(def z 1.1) ; scaling factor for automatically adjusting λ
(def MIN-N 10) ; Min. num. data points per series.
(def DEFAULT-SCALE const/DAY)
(def MAX-EPOCH 300); 200
(def keep-running (atom true))
(def resample (atom true))
(def SILENT (atom false))
(def SPREAD 2) ; The number of interval-size-ms that the blur effect will spread over. E.g. +- 2 days.
;
; Generic helpers
;
(defmacro println* [& args]
  `(when-not @SILENT
     (println ~@args)))

(defmacro print* [& args]
  `(when-not @SILENT
     (print ~@args)))

(defn warn-zero [v loc]
  (when (== 0 v)
    (println* "***WARNING***: Division by zero in" loc)))

;-----
;
; Needed for DP clustering algorithm.
;
(defn sample-partition-points [n a b]
  (apply sorted-set (map #(+ a (* (- b a) %)) (ist/sample-uniform n))))

(defn sample-intervals "generate random intervals over [0, 1)"
  [n]
  (-> (ist/sample-uniform (* 2 n)) sort))

(defn in-range [data start stop]
  (filter #(and (< start %) (< % stop)) data))

(defn length [[a b]]
  (- b a))

(defn inside? [interval]
  (let [start (first interval)
        stop  (second interval)]
    #(if (and (< start %) (< % stop)) % nil)))

(defn inside-any? [intervals]
  (apply some-fn (map inside? intervals)))

(defn get-points-contained-in [row a b]
  (apply sorted-set (subseq row >= a < b)))

(defn num-points-inside [row interval]
  (count (get-points-contained-in row interval)))

(defn diff-seq [row]
  (->> row sort (partition 2 1) (map #(apply - (reverse %)))))

(defn max-gap-for-interval [internal-points-augmented-by-boundaries]
  (apply max (diff-seq internal-points-augmented-by-boundaries)))

(defn make-intervals [points]
  (partition 2 1 points))

; @state interface

(defn get-start-time [the-state]
  (get the-state :start-time))

(defn get-stop-time [the-state]
  (get the-state :stop-time))

(defn sample-new-partition-points-from-the-state [the-state n]
 (let [a         (get-start-time the-state)
       b         (get-stop-time the-state)]
   (sample-partition-points n a b)))

(defn get-num-clusters [the-state]
  (count (get-in the-state [:C])))

(defn get-members [the-state]
  (let [members (map :members (get the-state :C))]
    (map #(into '() %) members)))

(defn get-members-of-cluster [the-state cluster-id]
  (get-in the-state [:C cluster-id :members]))

(defn get-num-members [the-state cluster-id]
  (count (get-members-of-cluster the-state cluster-id)))

(defn get-tag-ids [data]
  (-> data keys))

(defn get-num-tags [data]
  (count (get-tag-ids data)))

; An alias for mneumonic convenience.
(def get-tag-ids-of-cluster get-members-of-cluster)

(defn get-rates
  ([the-state]
    (get the-state :λ))
  ([the-state tag-id]
    (get-in the-state [:λ tag-id])))

(defn set-loglike
  ([the-state cluster-id tag-id like-new]
    (update-in the-state [:C cluster-id] set-loglike tag-id like-new))
  ([cluster tag-id like-new]
    (let [very-negative -999999999
          like-new (if (< like-new very-negative) very-negative like-new)]
      (assoc-in cluster [:loglike tag-id] like-new))))

(defn get-cluster-id [the-state tag-id]
  (get-in the-state [:phonebook tag-id]))

(defn get-partition-points-given-cluster-id [the-state cluster-id]
  (get-in the-state [:C cluster-id :partition-points]))

(defn get-intervals-given-cluster-id [the-state cluster-id]
  (make-intervals (get-partition-points-given-cluster-id the-state cluster-id)))

(defn get-partition-points-given-tag-id [the-state tag-id]
  (let [cluster-id (get-cluster-id the-state tag-id)]
    (get-partition-points-given-cluster-id the-state cluster-id)))

(defn get-intervals-given-tag-id [the-state tag-id]
  (make-intervals (get-partition-points-given-tag-id the-state tag-id)))

(defn get-clusters [the-state]
  (get the-state :C))

(defn get-cluster [the-state cluster-id]
  (get-in the-state [:C cluster-id]))

(defn get-phonebook [the-state]
  (get the-state :phonebook))

(defn get-counts-per-cluster [clusters]
  (vec (map #(count (get % :members)) clusters)))

(defn concat-partition-points [partition-points extra-points]
  (clojure.set/union partition-points extra-points))

(defn get-next-point [points point]
  (-> (subseq points > point)
      first list set))

(defn get-internal-points
  "Given a sorted-set of points, get only the internal points while preserving the sorted property of the set."
  [points]
  (let [first-point         (-> (first points) list set)
        last-point          (-> (last points) list set)]
    (-> points
      (clojure.set/difference first-point)
      (clojure.set/difference last-point))))

(defn only-end-points?
  "A predicate that determines that the sorted-set has only two points in it.  To have 'internal points' you need at least 3 members of the set."
  [points]
  (< (count (take 3 points)) 3))

(defn replace-point [partition-points extra-points]
  (if (only-end-points? partition-points)
      (concat-partition-points  partition-points extra-points)
      (let [internal-points     (get-internal-points partition-points)
            next-point          (get-next-point internal-points (first extra-points))]
        (-> partition-points
            (clojure.set/difference next-point)
            (clojure.set/union extra-points)))))

(defn delete-random-point
  "Given a sorted set, removed a random iternal point. (Remove anything but the first and last end-points.)"
  [partition-points extra-points]
  (if (only-end-points? partition-points)
    partition-points
    (let [num-internal-points  (- (count partition-points) 2)
          random-pos           (inc (rand-int num-internal-points))
          rand-elt             (-> partition-points vec (get random-pos) list set)]
      (clojure.set/difference partition-points rand-elt))))

(defn no-op-on-partition-points [partition-points extra-points] partition-points)

(def modify-partition-points { :no-op  no-op-on-partition-points
                               :create concat-partition-points
                               :update replace-point
                               :delete delete-random-point })

(defn get-partition-points-modified [partition-points extra-points op]
  ((get modify-partition-points op) partition-points extra-points))

(defn start-with-seq [start-with]
  (if (= :in start-with)
      '(:in :out)
      '(:out :in)))

(defn in-out-seq [n start-with]
  (->> (start-with-seq start-with) constantly repeatedly (take (/ n 2)) flatten (take n)))

(defn take-max-of-like-row
  ([like-seq start-with]
    (let [num-like-seq (count like-seq)
          key-seq (in-out-seq num-like-seq start-with)
          arg-seq (partition 2 2 (interleave key-seq like-seq))
          get-val (fn [[k like-pair]] (get like-pair k))]
      (map get-val arg-seq)))
  ([like-seq]
   (->> like-seq (map vals) (map #(apply max %)))))

;
; Likelihood related
;

(defn prob-n [n λ]
  (let [λ-safe (if (== 0 λ) 0.001 λ)]
    (ist/pdf-poisson n :lambda λ-safe)))

(defn like-interval [a b internal-points λ & {:keys [log] :or {log true}}]
  (let [num-internal-points  (count internal-points)
        like-count           (prob-n num-internal-points (* λ (- b a)))
        interval-points      (conj (conj internal-points a) b)
        max-gap              (max-gap-for-interval interval-points)
        like-gap             (ist/pdf-exp (max-gap-for-interval interval-points) :rate λ)]
    (if log
      (+ (ic/log like-count) (ic/log like-gap))
      (* like-count like-gap))))

; Change this length limit to 1% of (stop-date - start-date)
(defn penalize-narrow-intervals [x len]
  (if (< len 1.0)
      -100
      x))

; Getting the points within the boundaries of an interal is potentially O(n)
;   but get-points-contained-in does it in O(n*log(N_

(defn likes-interval [row interval λ-in λ-out & {:keys [log] :or {log true}}]
  (let [a                  (first interval)
        b                  (second interval)
        width              (- b a)
        internal-points    (get-points-contained-in row a b)
        like-in            (penalize-narrow-intervals (like-interval a b internal-points λ-in :log log)  width)
        like-out           (penalize-narrow-intervals (like-interval a b internal-points λ-out :log log) width)]
    ; Severely penalize intervals with a length less than 1.
    {:in  like-in
     :out like-out}))

(defn like-intervals-for-row [row intervals λ-in λ-out & {:keys [log] :or {log true}}]
  (map #(likes-interval row % λ-in λ-out :log log) intervals))

(defn like-for-row
  "The 'start-with' parameter is for existing clusters that have already determined whether the first interval is :in or :out."
  ([row intervals λ-in λ-out]
   (reduce + (take-max-of-like-row (like-intervals-for-row row intervals λ-in λ-out :log true))))
  ([row intervals λ-in λ-out start-with]
   (reduce + (take-max-of-like-row (like-intervals-for-row row intervals λ-in λ-out :log true)
                                  start-with))))

; IN/OUT labeling.

(defn in-or-out [like-hash]
  (if (> (:in like-hash) (:out like-hash)) :in :out))

(defn random-op [new-points]
  (let [n  (count (take 2 new-points))
        op (cond (= n 0) :delete
                 (= n 1) (get [:create :update] (rand-int 2))
                 :else :create)]
        op))

(defn sample-multinomial [weights]
  (first (ist/sample-multinomial 1 :probs weights)))

(defn sample-num-points []
  (first (ist/sample-multinomial 1 :probs [0.1 0.5 0.4])))


; Adjust lambda by multiples of 1.1, ensuring
;  0.001 < λ_out < 0.3
;  λ_in > 10 * λ_out
;  λ_in >= 1.0
;  multiplicative factor z >= 1, default is 1.1 (adjust by 10% each iteration by default).
;  Default value for λ_in is 2
;
(defn adjust-λ-out-op [λ_in λ_out z]
  (let [λ_out* (* λ_out z)]
    (cond (or (> λ_out* 0.3) (< λ_in (* 10 λ_out*) ))
            /
          (< λ_out* 0.001)
            *
          :else
            ([/ *] (rand-int 2)))))

(defn adjust-λ-in-op [λ_in λ_out z]
  (warn-zero z 'adust-λ-in-op)
  (let [λ_in* (/ λ_in z)]
    (if (or (< λ_in* 1.0)
            (< λ_in* (* 10 λ_out)))
        *
        ([/ *] (rand-int 2)))))

(defn adjust-λ-out [λ_in λ_out z]
  ((adjust-λ-out-op λ_in λ_out z) λ_out z))

(defn adjust-λ-in [λ_in λ_out z]
  ((adjust-λ-in-op λ_in λ_out z) λ_in z))

; Based on algorithm 7 of Radford M. Neal, 1998.
;
; Let state of markov chain consist of c_i, i=1..n
;  and ϕ = {ϕ_c : c ∈ {c_1, .. , c_n}, ϕ_i = (a_i, b_i), a random interval denoting the time boundaries of
;  the cluster.
;
; Example of state:
; ----------------
; `data` should be a map of sorted sets.  map to look up rows in O(1) by tag-id and
;   the row of data should be a sorted set to get performan range queries
;   to get the subset of time points in the row contained inside a sub iterval
;   in O(log(total num time points in row) * (length of subset)) with Clojure's
;   subseq function which only works on sorted collections (sorted sets or sorted
;   maps).
;
; C -> vector tag clusters
;   each tag cluster consists of
;   i.)  a set of member tags
;   ii.) a subset of partition points randomly drawn the time interval under consideration.
; phonebook -> an index of tag-ids to cluster id for fast O(1) look up by tag-id.
; λ -> hash-map of tag-id -> {:λ_in 42, λ_out 0.1} inside/outside rates
;
; {C: [{:members #{42, 3, 5}
;       :partition-points (sorted-set 11.3 42.9)}]
;  λ: (sorted-set)}
;  phonebook: {42 0 99 0 100 1 101 1}
;
; INPUT:
;   start date-time (t_0)
;   stop date-time (t_K)
;   data: a hash-map of tag-id -> seq of date-times as numbers.
;

; Reporting functions.

;(in-outs-for-cluster data @state 0 '(10 23 28 40))

; Initialization
(defn estimate-in-rate [row]
  (let [median-diff (ist/median (diff-seq row))
        ln2         (ic/log 2)]
    (warn-zero median-diff 'estimate-in-rate)
    (/ ln2 (* 0.45 median-diff))))

(defn like-to-p
  "Convert the log-likelihood back to a probability."
  [log-like]
  (ic/exp log-like))

; Assume the max gap size represents about the 90th percentile of the true gap distribution
;   of a constant Poisson process.
;   c.f. the quantile formula of the exponential distribution.
(defn estimate-out-rate [row]
  (let [max-diff    (apply max (diff-seq row))
        ; Obviously, this is not going to be that accurate, but luckily we just need the
        ;   rates to be in the approximate neighborhood to get the likelihood to converge.
        max-diff-is-what-percentile-of-the-gap-density 0.9]
    (warn-zero max-diff 'estimate-out-rate)
    (* -1
       (ic/log max-diff-is-what-percentile-of-the-gap-density)
       (/ 1.0 max-diff))))

(defn init-rates [data]
  (let [tag-ids  (keys data)
        est-rate (fn [tag-id]
                   (let [row   (get data tag-id)
                         λ-in  (estimate-in-rate row)
                         λ-out (estimate-out-rate row)]
                     (hash-map tag-id {:in λ-in :out λ-out})))]
    (reduce into {} (map est-rate tag-ids))))

(defn init-cluster [start-time stop-time tag-ids]
  (let [tag-list (if (not (seq? tag-ids)) (list tag-ids) tag-ids)]
    {:start-with nil
     :loglike (into {} (map #(hash-map % nil) tag-list))
     :members (apply sorted-set tag-list)
     :partition-points (sorted-set start-time stop-time)}))

(defn resample-row [row partition-points λ-in λ-out start-time stop-time like-before iter max-iter]
  (if (< max-iter iter)
      {:partition-points partition-points}
      (let [random-points         (sample-partition-points (sample-num-points) start-time stop-time)
            op                    (random-op random-points)
            new-partition-points  (get-partition-points-modified partition-points random-points op)
            like-after            (like-for-row row (make-intervals new-partition-points) λ-in λ-out)]
        (if (> like-before like-after)
            (recur row partition-points λ-in λ-out start-time stop-time like-before (inc iter) max-iter)
            (recur row new-partition-points λ-in λ-out start-time stop-time like-after (inc iter) max-iter)))))

(defn resample-tag [data the-state tag-id max-iter]
  (let [row (get data tag-id)
        partition-points (sorted-set (get-start-time the-state) (get-stop-time the-state))
        rate (get-rates the-state tag-id)
        start-time (get-start-time the-state)
        stop-time (get-stop-time the-state)
        like-before -999999]
    (assoc (resample-row row partition-points (:in rate) (:out rate) start-time stop-time like-before 0 max-iter)
           :members (sorted-set tag-id))))

(defn resample-tag-with-cluster-points [data the-state cluster-id tag-id max-iter]
  (let [row (get data tag-id)
        partition-points (get-in the-state [:C cluster-id :partition-points])
        rate (get-rates the-state tag-id)
        start-time (get the-state :start-time)
        stop-time (get the-state :stop-time)
        like-before (like-for-row row (make-intervals partition-points) (:in rate) (:out rate))]
    (resample-row row partition-points (:in rate) (:out rate) start-time stop-time like-before 0 max-iter)))

(defn get-start-with
  "Given enough information to compute the first interval's log likelihood, decide whether it is an :in interval or an :out interval.  (This will be used to determine the the other in/out state of the partition by alternating in-out."
  [row partition-points rate]
  (let [first-interval (first (make-intervals partition-points))
        likes-first (likes-interval row first-interval (:in rate) (:out rate) :log true)]
    (-> (im/sort-map-by-value likes-first) last first)))

(defn resample-singletons [data state max-iter]
  (doseq [tag-id (get-tag-ids data)]
    (print* (str "resample tag#" tag-id))
    (let [the-state         @state
          cluster-id        (get-cluster-id the-state tag-id)
          partition-points  (get (resample-tag-with-cluster-points data the-state cluster-id tag-id max-iter) :partition-points)
          rate              (get-rates the-state tag-id)
          intervals-new     (make-intervals partition-points)
          row               (get data tag-id)
          like-new          (like-for-row row intervals-new (:in rate) (:out rate))
          start-with        (get-start-with row partition-points rate)
          new-state         (assoc-in the-state [:C cluster-id :start-with] start-with)
          new-state         (assoc-in new-state [:C cluster-id :partition-points] partition-points)
          new-state         (set-loglike new-state cluster-id tag-id like-new)]
      (println* " " (get-in new-state [:C cluster-id :partition-points]))
      (reset! state new-state))))

(defn update-rate! [state tag-id in-or-out rate]
  (swap! state #(assoc-in % [:λ tag-id in-or-out] rate)))

(defn base-points [the-state]
  (list (get-start-time the-state) (get-stop-time the-state)))

(defn base-intervals [the-state]
  (make-intervals (base-points the-state)))

(defn remove-tag-from-cluster
  ([the-state tag-id]
    (remove-tag-from-cluster the-state (get-cluster-id the-state tag-id) tag-id))
  ([the-state cluster-id tag-id]
    (let [the-state (im/dissoc-in the-state [:C cluster-id :loglike tag-id])
          the-state (update-in the-state [:C cluster-id :members] #(clojure.set/difference % (set (list tag-id))))]
      the-state)))

(defn get-clusters-minus [the-state tag-id]
  (let [new-state (remove-tag-from-cluster the-state tag-id)]
    (get-clusters new-state)))

(defn add-tag-to-cluster
  ([cluster tag-id like-new]
    (let [cluster (set-loglike cluster tag-id like-new)
          cluster (update-in cluster [:members] #(clojure.set/union % (set (list tag-id))))]
      cluster))
  ([the-state cluster-id tag-id like-new]
    (update-in the-state [:C cluster-id] add-tag-to-cluster tag-id like-new)))

(defn append-new-cluster [the-state new-cluster]
  (update-in the-state [:C] conj new-cluster))

(defn make-phonebook [clusters]
  (let [members      (vec (map :members clusters))
        the-range    (range (count members))
        index-map    (fn [i]
                       (map #(sorted-map % i)
                            (members i)))
        loc-seq      (flatten (map index-map the-range))
        phonebook    (into (sorted-map) loc-seq)]
    phonebook))

(defn update-phonebook
  ([the-state]
    (let [new-phonebook (make-phonebook (get the-state :C))]
      (assoc the-state :phonebook new-phonebook)))
  ([the-state tag-id cluster-id]
    (assoc-in the-state [:phonebook tag-id] cluster-id)))

(defn initialize-state [data state start-time stop-time]
  ; Start with one cluster with all members in it, and
  ;   the end points of entire interval under consideration as the partition boundary points.
  ; The phonebook tells you which cluster a tag-id is in in O(1) time.
  ;   It needs to be updatd everytime tag-ids as assigned to different clusters.
  (let [rates (init-rates data)
        the-state {:C  (vec (map #(init-cluster start-time stop-time %) (get-tag-ids data)))
                   :start-time start-time
                   :stop-time stop-time
                   :λ rates}
        the-state (update-phonebook the-state)]
    (reset! state the-state)))

(defn new-state-move-non-singleton-to-new-cluster [the-state new-cluster tag-id like-new]
  " remove tag-id from old cluster.
    append new cluster to the vector of clusters
    update the index of tag-ids to clusters to point to the new cluster-id"
  (let [new-cluster*      (add-tag-to-cluster new-cluster tag-id like-new)
        old-cluster-id    (get-cluster-id the-state tag-id)
        new-cluster-id    (get-num-clusters the-state)
        new-state         (remove-tag-from-cluster the-state old-cluster-id tag-id)
        new-state         (append-new-cluster new-state new-cluster*)
        new-state         (update-phonebook new-state tag-id new-cluster-id)]
    new-state))

(defn clean-clusters [clusters]
  (vec (remove #(empty? (get % :members)) clusters)))

(defn- move-tag-to-new-cluster [the-state new-cluster-id tag-id]
  "Without updating the phonebook or cleaning up empty clusters, just move the tag-id from one cluster to another."
  (let [old-cluster-id (get-cluster-id the-state tag-id)]
    (if (= old-cluster-id new-cluster-id)
        the-state
        (let [new-state         (add-tag-to-cluster the-state new-cluster-id tag-id)
              new-state         (remove-tag-from-cluster new-state old-cluster-id tag-id)]
          new-state))))

(defn new-state-move-tag [the-state new-cluster-id tag-id like-new]
  (let [old-cluster-id (get-cluster-id the-state tag-id)]
    (if (= old-cluster-id new-cluster-id)
        ; Just return the previous state if you're moving a tag to the same cluster.
        the-state
        (let [new-state         (add-tag-to-cluster the-state new-cluster-id tag-id like-new)
              new-state         (remove-tag-from-cluster new-state old-cluster-id tag-id)
              new-state         (update-in new-state [:C] clean-clusters)
              new-state         (update-phonebook new-state)]
          new-state))))
;
; transition probabilities
;

(defn pr-transition-non-singleton-to-new [p-new p-old N alpha]
  (warn-zero p-old 'pr-transition-non-singleton-to-new)
  (cond (== 0 p-old) 1
        (== 0 p-new) 0
        :else (min 1 (* (/ alpha (dec N)) (/ p-new p-old)))))

(defn pr-transition-singleton-to-existing [p-new p-old N alpha]
  (warn-zero p-old 'pr-transition-singleton-to-existing)
  (cond (== 0 p-old) 1
        (== 0 p-new) 0
        :else (min 1 (* (/ (dec N) alpha) (/ p-new p-old)))))

(defn sample-transition-non-singleton-to-new-cluster? [like-new like-old N alpha]
  (let [p-new (like-to-p like-new)
        p-old (like-to-p like-old)]
      (< (first (ist/sample-uniform 1))
         (pr-transition-non-singleton-to-new p-new p-old N alpha))))

(defn sample-transition-singleton-to-existing-cluster? [like-new like-old N alpha]
  (let [p-new (like-to-p like-new)
        p-old (like-to-p like-old)]
  (< (first (ist/sample-uniform 1))
     (pr-transition-singleton-to-existing p-new p-old N alpha))))

(defn normalize-counts [v]
  (let [sum (reduce + v)]
    (warn-zero sum 'normalize-counts)
    (map (partial * (/ 1 sum)) v)))

(defn sample-cluster-id-from-clusters-minus [the-state tag-id]
  "Remove tag-id from the state vctor, then choose a random cluster id from the remaining
  in proportion to the member count."
  (let [the-clusters     (get-clusters the-state)
        cluster-counts   (get-counts-per-cluster the-clusters)
        cluster-id       (get-cluster-id the-state tag-id)
        cluster-counts   (assoc cluster-counts cluster-id 0) ; remove counts of the tag's cluster.
        prob-vec         (vec (normalize-counts cluster-counts))]
    (first (ist/sample-multinomial 1 :probs prob-vec))))

;
; assign
;

(defn assign-singleton-to-existing-cluster [data the-state tag-id N alpha]
  "If there is more than one cluster, reassign the tag to a different cluster with a probability
  in proportion to the size of the other clusters."
  (when (> (get-num-clusters the-state) 1)
    (let [row              (get data tag-id)
          rate             (get-rates the-state tag-id)
          old-cluster-id   (get-cluster-id the-state tag-id)
          intervals-old    (get-intervals-given-cluster-id the-state old-cluster-id)
          start-with-old   (get-in the-state [:C old-cluster-id :start-with])
          like-old         (like-for-row row intervals-old (:in rate) (:out rate) start-with-old)

          new-cluster-id   (sample-cluster-id-from-clusters-minus the-state tag-id)
          intervals-new    (get-intervals-given-cluster-id the-state new-cluster-id)
          start-with-new   (get-in the-state [:C new-cluster-id :start-with])
          like-new         (like-for-row row intervals-new (:in rate) (:out rate) start-with-new)
          new-state        (new-state-move-tag the-state new-cluster-id tag-id like-new) ]
      (when (sample-transition-singleton-to-existing-cluster? like-new like-old N alpha)
        new-state))))

(defn assign-non-singleton-to-new-cluster
  "Return the new cluster for the tag, if it passes the transition probabilitly.
  Move a tag from a non-singleton cluster to a newly created cluster.
  Update the state-atom only if the likelihood improves or by a small probability
  proportional to the alpha parameter and the ratio of the likelihood of the proposed state
  to the existing state.  We use probabilities since log-likelihoods would cause a lot
  of flopping around "
  [data the-state tag-id N alpha max-iter]
  (let [old-cluster-id        (get-cluster-id the-state tag-id)
        old-cluster           (get-cluster the-state old-cluster-id)
        first-cluster-id      (get-in the-state [:first-phonebook tag-id])
        new-cluster           (get-in the-state [:first-clusters first-cluster-id])
        rate                  (get-rates the-state tag-id)
        row                   (get data tag-id)
        start-with-old        (get-in the-state [:C old-cluster-id :start-with])
        intervals-old         (get-intervals-given-cluster-id the-state old-cluster-id)
        partition-points-new  (get new-cluster :partition-points)
        intervals-new         (make-intervals partition-points-new)
        start-with-new        (get-start-with row partition-points-new rate)
        new-cluster           (assoc new-cluster :start-with start-with-new)
        like-old              (like-for-row row intervals-old (:in rate) (:out rate) start-with-old)
        like-new              (like-for-row row intervals-new (:in rate) (:out rate) start-with-new)]
    (when (sample-transition-non-singleton-to-new-cluster? like-new like-old N alpha)
          (new-state-move-non-singleton-to-new-cluster the-state new-cluster tag-id like-new))))

(defn singleton? [data the-state tag-id]
  (let [cluster-id        (get-cluster-id the-state tag-id)
        cluster           (get-cluster the-state cluster-id)
        members           (get cluster :members)]
    (= (count (take 2 members)) 1)))

(defn pp-num [x]
  (/ (math/round (* 10 x)) 10.0))

(defn sort-clusters-by-num-partition-points [clusters]
  (sort-by #(+ (count (:partition-points %))
               (if (= :out (:start-with %)) 0.5 0.0))
           clusters))

(defn print-cluster [cluster data rates]
  (let [points (get cluster :partition-points)
        intervals (make-intervals points)]
    (print* (map #(pp-num %) points))
    (println* "")
    (doseq [tag-id  (into '() (get cluster :members))]
      (let [row     (get data tag-id)
            rate    (get rates tag-id)
            like    (like-for-row row intervals (:in rate) (:out rate) (:start-with cluster))]
        (println* "       like for tag id#" tag-id " : " like)))))

(defn print-state [data the-state user-id]
  (let [clusters (get the-state :C)]
    (print* "*** CLUSTERING USER #" user-id ": ")
    (println* (map #(get % :members) clusters))))

(defn all-points-within-epsilon? [epsilon points1 points2]
  (every? #(< (math/abs (apply - %)) epsilon) (partition 2 2 (interleave points1 points2))))

(defn average-partition-points [points1 points2]
  (apply sorted-set (map #(/ (apply + %) 2.0) (partition 2 2 (interleave points1 points2)))))

; Preserve other attributes of cluster 1 while updating :members and :partition-points with
;   the values of cluster 2.
(defn union-two-clusters [c1 c2]
  (let [new-cluster (update-in c1 [:members] clojure.set/union (:members c2))
        new-cluster (update-in new-cluster [:partition-points] average-partition-points (:partition-points c2))]
    new-cluster))

(defn merge-two-clusters [c1 c2]
  (cond (and (nil? c1) (nil? c2))
        '()

        (nil? c1)
        (list c2)

        (nil? c2)
        (list c1)

        (or (not (= (:start-with c1)
                    (:start-with c2)))
            (not (= (count (:partition-points c1))
                    (count (:partition-points c2)))))
        (list c1 c2)

        (all-points-within-epsilon? 1.0 (:partition-points c1) (:partition-points c2))
        (list (union-two-clusters c1 c2))

        :else
        (list c1 c2)))

(defn merge-clusters
  ([merged-clusters unmerged-clusters]

   (cond (= 0 (count merged-clusters))
         (recur (vec (list (first unmerged-clusters)))
                (rest unmerged-clusters))

         (= 0 (count unmerged-clusters))
         (vec (flatten merged-clusters))

         :else
         (let [next-cluster (merge-two-clusters (last merged-clusters) (first unmerged-clusters))]
           (cond (= 1 (count next-cluster))
                 (recur (assoc (vec merged-clusters) (dec (count merged-clusters)) (first next-cluster))
                        (rest unmerged-clusters))

                 (= 2 (count next-cluster))
                 (recur (conj (vec merged-clusters) (first unmerged-clusters))
                        (rest unmerged-clusters))

                 :else
                 (recur merged-clusters '()))))))

(defn merge-redundant-clusters [state]
  (let [the-state @state
        sorted-clusters  (sort-clusters-by-num-partition-points (:C the-state))
        merged-clusters (merge-clusters '() sorted-clusters)
        new-state (assoc-in the-state [:C] merged-clusters)
        new-state (update-phonebook new-state)]
    (reset! state new-state)))

(defn loglike-for-cluster [cluster data rates]
  (let [start-with (get cluster :start-with)
        members (get cluster :members)
        intervals (make-intervals (get cluster :partition-points))]
    (into {} (map (fn [tag-id]
                    (let [row (get data tag-id)
                          rate (get rates tag-id)
                          like-new (like-for-row row intervals (:in rate) (:out rate) start-with)]
                      (hash-map tag-id like-new)))
                  members))))

(defn compute-loglikes [data the-state]
  (let [clusters (get the-state :C)
        rates (get-rates the-state)]
    (vec (map #(assoc % :loglike (loglike-for-cluster % data rates)) clusters))))

(defn update-loglikes [data state]
  (swap! state assoc :C (compute-loglikes data @state)))

(defn refurbish-clusters [data state]
  (merge-redundant-clusters state)
  (update-loglikes data state))

(defn evolve-alpha [epoch alpha]
 (* alpha (Math/pow 100 (- (* 1.0 (Math/log10 epoch))))))

(defn save-original-sampling [state]
  (let [the-state @state]
    (swap! state assoc-in [:first-clusters] (get the-state :C))
    (swap! state assoc-in [:first-phonebook] (make-phonebook (get the-state :C)))))

(defn cluster-fit [clusters]
   (->> clusters
        (map :loglike)
        (map vals)
        flatten
        (sort >)
        (reduce +)))

(defn avg-cluster-fit [clusters n]
  (/ (cluster-fit clusters) n))

(defn percent-fit [state n]
  (/ (avg-cluster-fit (:first-clusters @state) n)
     (avg-cluster-fit (:C @state) n)))

(defn process-singleton [data state tag-id N alpha]
  (when-let [new-state (assign-singleton-to-existing-cluster data @state tag-id N alpha)]
    (reset! state new-state)
    (refurbish-clusters data state)))

(defn process-non-singleton [data state tag-id N alpha max-iter-new-cluster]
  "Assign tag back to original partition if it passes the transition probability sampling."
  (when-let [new-state (assign-non-singleton-to-new-cluster data @state tag-id N alpha max-iter-new-cluster)]
    (reset! state new-state)))

(defn cluster-counts [clusters]
  (->> clusters (map :members) (map count)))

(defn algorithm-7-Neal-2000
  ([data state max-epoch] (algorithm-7-Neal-2000 data state MAX-ITER-NEW-CLUSTER α MAX-ITER-NEW-CLUSTER))
  ([data state max-epoch alpha max-iter-new-cluster & [user-id]]
    (let [N (count data)]
      (println* "Sample partitions for each tag.")
      (when @resample (resample-singletons data state max-iter-new-cluster))
      ; Save original sampling of partitions to (dramatically) speed up the clustering.
      (save-original-sampling state)
      (refurbish-clusters data state)
      (dotimes [iter max-epoch]
        (when @keep-running
          (doseq [tag-id (get-tag-ids data)]
            (if (singleton? data @state tag-id)
              (process-singleton data state tag-id N alpha)
              (process-non-singleton data state tag-id N alpha max-iter-new-cluster)))
          (print* "Finished Epoch #" iter ".")
          (print* " fitness:" (percent-fit state N))
          (println* " -- "  (cluster-counts (:C @state)))))
      (swap! state assoc-in [:best-clusters] (get @state :C))
      (update-loglikes data state)
      nil)))

; series to data-map as required by algorithm above.
(defn to-data-map [series]
  (im/apply-to-values #(apply sorted-set %)
                      (im/collect-into-index series :tag_id :date)))

(defn descale-time [x interval-size-in-ms]
  (tr/to-sql-time (tr/from-long (long (* x interval-size-in-ms)))))

; seq of maps with :date (datetime) -> seq of maps with :date (number)
(defn dates-to-numbers [series]
  (im/update-in-map-series series [:date] tr/to-long))

; series (seq of maps with :date (datetime)) -> series with :date (number, scaled by interval-size-ms)
(defn dates-to-scale [series interval-size-ms]
  "Coerce date-time into a number divided by the scale.  'interval-size-ms' can be const/DAY for example."
  (im/update-in-map-series series [:date] #(db/rescale-time % interval-size-ms)))

(defn prepare-data [series interval-size-ms]
  (-> series
      (dates-to-scale interval-size-ms)
      to-data-map))

(defn data-series-list [interval-size-ms & args]
  (prepare-data (apply db/series-list args) interval-size-ms))

(defn data-series-range [interval-size-ms & args]
  (prepare-data (apply db/series-range args) interval-size-ms))

(defn data-series-range-with-min-n [min-n interval-size-ms & args]
  (let [data-map               (apply data-series-range (cons interval-size-ms args))
        small-tags-with-nils   (map (fn [[k v]] (when (< (count v) min-n) k)) data-map)
        small-tags             (filter #(not (nil? %)) small-tags-with-nils)]
    (apply dissoc data-map small-tags)))

(defn str-date [date]
  (let [joda-date (if (= java.sql.Timestamp (class date))
            (tr/from-sql-time date)
            date)]
    (tf/unparse (tf/formatter "yyyy-MM-dd_HH:mm:ss") joda-date)))

(defn run-algo [user-id max-epoch state min-n interval-size-ms start-time stop-time alpha max-iter-new-cluster]
  (let [start-time-as-decimal (db/rescale-time start-time interval-size-ms)
        stop-time-as-decimal (db/rescale-time stop-time interval-size-ms)
        data (data-series-range-with-min-n min-n interval-size-ms start-time stop-time user-id)]
    (reset! keep-running true)
    (when (> (count data) 0)
      (println* "initializing user" user-id)
      (when @resample (initialize-state data state start-time-as-decimal stop-time-as-decimal))
      (println* "running" max-epoch "epochs")
      (algorithm-7-Neal-2000 data state max-epoch alpha max-iter-new-cluster user-id)
      (println* "finished clustering for user" user-id "over period:" (str-date start-time) "-" (str-date stop-time))
      (when-let [best-clusters (get @state :best-clusters)]
        (swap! state assoc-in [:C] best-clusters))
      nil)))

(defn terminate []
  (reset! keep-running false))

; *** Saving to database ***

(defn descale-time [x interval-size-in-ms]
  (tr/to-sql-time (tr/from-long (long (* x interval-size-in-ms)))))

(defn cluster-interval-save-many [tag-cluster-id intervals start-with interval-size-ms]
  (let [in-out-list (in-out-seq (count intervals) start-with)]
    (doseq [args (partition 2 2 (interleave in-out-list intervals))]
      (let [in-out (first args)
            interval (last args)
            iv-start-date (descale-time (first interval) interval-size-ms)
            iv-stop-date (descale-time (last interval) interval-size-ms)]
        (when (= :in in-out)
          (db/cluster-interval-create tag-cluster-id iv-start-date iv-stop-date))))))

(defn tag-cluster-add-tags [tag-cluster-id members loglikes]
  (doseq [tag-id members]
    (db/tag-cluster-add-tag tag-cluster-id tag-id (get loglikes tag-id))))

(defn tag-cluster-save-substructure [cluster-run-id state-cluster interval-size-ms make-intervals]
  (let [members           (get state-cluster :members)
        intervals         (make-intervals (get state-cluster :partition-points))
        start-with        (get state-cluster :start-with)
        tag-cluster-id    (db/tag-cluster-create cluster-run-id)
        loglikes          (get state-cluster :loglike)]
    (tag-cluster-add-tags tag-cluster-id members loglikes)
    (cluster-interval-save-many tag-cluster-id intervals start-with interval-size-ms)))

(defn save-clusters [cluster-run-id state interval-size-ms make-intervals]
  (doseq [state-cluster (get @state :C)]
    (tag-cluster-save-substructure cluster-run-id state-cluster interval-size-ms make-intervals)))

(defn save-intervals-for-user
  ([user-id cluster-run-parent-id]
    (let [start-time db/BIG-BANG
          stop-time (db/sql-now)]
      (save-intervals-for-user user-id cluster-run-parent-id start-time stop-time)))
  ([user-id cluster-run-parent-id start-time stop-time]
    (let [max-epoch MAX-EPOCH
          state (atom {})
          min-n MIN-N
          interval-size-ms const/DAY
          alpha α
          max-iter-new-cluster MAX-ITER-NEW-CLUSTER]
      (save-intervals-for-user user-id cluster-run-parent-id max-epoch state min-n interval-size-ms start-time stop-time alpha max-iter-new-cluster)))
  ([user-id cluster-run-parent-id start-time stop-time max-epoch state min-n interval-size-ms alpha max-iter-new-cluster]
    (let [cluster-run-id (db/cluster-run-create user-id :cluster-run-parent-id cluster-run-parent-id :start-date start-time :stop-date stop-time :min-n min-n :interval-size-ms interval-size-ms )]
      (run-algo user-id max-epoch state min-n interval-size-ms start-time stop-time alpha max-iter-new-cluster)
      (save-clusters cluster-run-id state interval-size-ms make-intervals)
      (db/cluster-run-update-finished cluster-run-id))))

(defn now-in-ms [] (-> (db/sql-now) tr/to-long))

(declare rescale-numeric-time)
(defn now-index [interval-size-ms]
  (rescale-numeric-time interval-size-ms (now-in-ms)))

(defn skip-cluster-run? [user-id]
  (when-let [last-run (first (db/cluster-run-list-finished user-id))]
    (let [one-day-ms  const/DAY
          last-run-ms (-> last-run :finished tr/to-long) ]
      (< (- (now-in-ms) last-run-ms)
         one-day-ms))))

(defn save-cluster-year-by-year [user-id & {:keys [max-epoch state min-n interval-size-ms alpha max-iter-new-cluster]
                                             :or {max-epoch MAX-EPOCH
                                                  state (atom {})
                                                  min-n MIN-N
                                                  interval-size-ms const/DAY
                                                  alpha α
                                                  max-iter-new-cluster MAX-ITER-NEW-CLUSTER }}]
  (let [cluster-run-parent-id (db/cluster-run-create user-id)
        next-year (inc (clj-time.core/year (clj-time.core/now)))]
    (doseq [year (range 2010 next-year)]
      (when @keep-running
        (println* "\n\n\n**************\n\nSTART save-intervals-for-user:" user-id " year: " year)
        (save-intervals-for-user user-id cluster-run-parent-id (db/sql-time year 1 1) (db/sql-time year 12 31 23 59 59)
                                 max-epoch state min-n interval-size-ms alpha max-iter-new-cluster )))))

(defn refresh-intervals-for-user [user-id & {:keys [start-time stop-time
                                                    max-epoch state min-n
                                                    interval-size-ms alpha max-iter-new-cluster]
                                             :or {start-time nil
                                                  stop-time nil
                                                  max-epoch MAX-EPOCH
                                                  state (atom {})
                                                  min-n MIN-N
                                                  interval-size-ms const/DAY
                                                  alpha α
                                                  max-iter-new-cluster MAX-ITER-NEW-CLUSTER }}]
  (if (and start-time stop-time)
      (save-intervals-for-user user-id nil start-time stop-time
                               max-epoch state min-n interval-size-ms alpha max-iter-new-cluster)
      (save-cluster-year-by-year user-id
                                 :max-epoch max-epoch
                                 :min-n min-n
                                 :interval-size-ms interval-size-ms
                                 :alpha alpha
                                 :max-iter-new-cluster max-iter-new-cluster)))

(defn intervals-for-tag [user-id tag-id]
  (-> (db/interval-list-as-numeric user-id tag-id) identity))

(defn join-intervals [intervals1 intervals2]
  (->> (concat intervals1 intervals2)
       (sort-by first)))

(defn merge-first-two? [intervals]
  (let [l (-> intervals first first)
        r (-> intervals first second)
        l2 (-> intervals second first)]
    (<= l2 r)))

(defn merge-first-two [intervals]
  (let [i1 (first intervals)
        i2 (second intervals)]
    (list (first i1) (max (second i1) (second i2)))))

(defn union-intervals
  ([intervals]
    (cond (< (count intervals) 2)
            intervals
          (merge-first-two? intervals)
            (cons (merge-first-two intervals)
                  (union-intervals (rest (rest intervals))))
          :else
            (cons (first intervals) (union-intervals (rest intervals)))))
  ([intervals1 intervals2]
    (union-intervals (join-intervals intervals1 intervals2))))

(defn rescale-numeric-time [interval-size-ms numeric-date-time-in-ms-since-1970]
  (double (/ numeric-date-time-in-ms-since-1970 interval-size-ms)))

(defn numeric-time [interval-size-ms & args]
  (->> (apply tc/date-time args)
      tr/to-long
      (rescale-numeric-time interval-size-ms)))

(defn rescale-numeric-time-interval [interval-size-ms interval-in-ms]
  (map (partial rescale-numeric-time interval-size-ms) interval-in-ms))

(defn rescale-intervals [interval-size-ms intervals]
  (map (partial rescale-numeric-time-interval interval-size-ms) intervals))

(defn intersected-intervals [user-id tag1-id tag2-id]
  (let [intervals1 (db/interval-list-as-numeric user-id tag1-id)
        intervals2 (db/interval-list-as-numeric user-id tag2-id)]
    (dso/intersect-intervals intervals1 intervals2)))

(defn rescaled-intersected-intervals [interval-size-ms user-id tag1-id tag2-id]
  (let [intervals (intersected-intervals user-id tag1-id tag2-id)]
    (rescale-intervals interval-size-ms intervals)))

(defn now-formatted []
  (tf/unparse (tf/formatter "yyyy-MM-dd_HH:mm:ss") (tc/now)))

(defn log [log-file-name message]
  (println* "core/log (" log-file-name "):" message)
  (when log-file-name
    (spit log-file-name
          (str "Clojure Interop " (now-formatted) " : " message "\n")
          :append true)))

; Input a sorted map, and select for keys in the range of the interval.
(defn select-in-interval [sorted interval]
    (subseq sorted >= (first interval) <= (last interval)))

(defn binify [user-id tag-id]
  (into (sorted-map) (-> (db/series-list user-id tag-id)
                         (bi/binify-by-avg const/DAY))))

(defn fill-in-series [bins start-date stop-date data-type]
  (if (= "CONTINUOUS" data-type)
      (bi/continuous-series bins start-date stop-date)
      (bi/event-series bins start-date stop-date)))

(defn to-sorted-map [m]
  (let [filtered (filter identity m)
        vected   (vec filtered)
        flattened (flatten vected)
        sorted (apply sorted-map flattened)]
    sorted))

(defn select-values-in-intervals [filled-sorted-map intervals]
  (let [selected (map (partial select-in-interval filled-sorted-map) intervals)]
    (to-sorted-map selected)))

(defn smooth-bins [filled-bins n]
  "Apply a smoothing function to a sorted-map of time-amount values and return the result as a sorted-map."
  (im/assert-type filled-bins clojure.lang.PersistentTreeMap)
  (let [ks (keys filled-bins)
        vs (vals filled-bins)
        smoothed-vals (stats/smooth vs n)]
    (into (sorted-map) (zipmap ks smoothed-vals))))

(defn replace-keys [smap new-ks]
  "Replace the keys of a sorted map."
  (into (sorted-map) (zipmap new-ks (vals smap))))

(defn replace-vals [smap new-vals]
  "Replace the vals of a sorted map."
  (into (sorted-map) (zipmap (keys smap) new-vals)))
  
(defn shift-keys [bins n]
  "Shift the numeric keys of a sorted-map to the left(n<0) or right (n>0)."
  (im/assert-type bins clojure.lang.PersistentTreeMap)
  (replace-keys bins (map (partial + n) (keys bins))))

(defn binify-shift-fill [user-id tag-id n]
  (-> (binify user-id tag-id)
      (shift-keys n)))

; This is where to put logic for pre-transforming time-series for the same-day correlation computation.
(defn vectorize-in-intervals-cor [user-id tag-id start-date-index stop-date-index rescaled-intervals x-or-y]
  (let [data-type          (db/series-data-type user-id tag-id)]
    (-> (binify user-id tag-id)
        (fill-in-series start-date-index stop-date-index data-type)
        (smooth-bins SPREAD)
        (select-values-in-intervals rescaled-intervals)
        vals)))

(defmacro if-target [target x-or-y a b] `(if (= ~target ~x-or-y) ~a ~b))
(defn if-x [x-or-y a b] (if-target :x x-or-y a b))
(defn if-y [x-or-y a b] (if-target :y x-or-y a b))

(defmacro if-bins [if-fn bins x-or-y f args]
  `(~if-fn ~x-or-y
           (apply ~f (cons ~bins ~args))
           ~bins))

(defn if-x-bins [bins x-or-y f & args]
  (if-bins if-x bins x-or-y f args))

(defn if-y-bins [bins x-or-y f & args]
  (if-bins if-y bins x-or-y f args))

(defn change-point-spike-train [bins]
  "A wrapper around detect-changes/change-point-spike-train to take in hash-maps instead of vectors."
  (replace-vals bins (dc/change-point-spike-train (vals bins))))

(defn shift-intervals [intervals n]
  "Shift a list of intervals to the right (n>0) or left (n<0)."
  (map #(map (partial + n) %) intervals))

; This is where to put logic for pre-transforming time-series for the trigger computation.
(defn vectorize-in-intervals-trigger [user-id tag-id start-date-index stop-date-index rescaled-intervals x-or-y]
  (let [data-type          (db/series-data-type user-id tag-id)
        stop-date-index'   (+ 1 stop-date-index)
        rescaled-intervals (if-x x-or-y (shift-intervals rescaled-intervals 1) rescaled-intervals)]
    (-> (binify user-id tag-id)
        (if-x-bins x-or-y shift-keys +1)
        (fill-in-series start-date-index stop-date-index' data-type)
        (smooth-bins SPREAD)
        (if-y-bins x-or-y change-point-spike-train)
        (select-values-in-intervals rescaled-intervals)
        vals)))

(defn compute-correlation-helper [values1 values2 tag1-id tag2-id]
    (if (stats/zero-cor? values1 values2)
        {:score 0
         :n 0
         :tag1_id tag1-id
         :tag2_id tag1-id}
        {:score (stats/cor values1 values2)
         :n     (count values1)
         :tag1_id tag1-id
         :tag2_id tag1-id }))

(defn values-in-intervals [vectorize-fn user-id tag1-id tag2-id rescaled-intervals]
  (let [start-date-index (->> rescaled-intervals flatten (apply min) long)
        stop-date-index  (->> rescaled-intervals flatten (apply max) long)
        x-values (vectorize-fn user-id tag1-id start-date-index stop-date-index rescaled-intervals :x)
        y-values (vectorize-fn user-id tag2-id start-date-index stop-date-index rescaled-intervals :y)]
    {:x x-values :y y-values}))

(defn compute-correlation [vectorize-fn user-id tag1-id tag2-id]
  "Compute the correlation cor(x, y)"
  (let [rescaled-intervals (rescaled-intersected-intervals const/DAY user-id tag1-id tag2-id)]
    (when (> (count rescaled-intervals) 0)
      (let [values (values-in-intervals vectorize-fn user-id tag1-id tag2-id rescaled-intervals)]
        (compute-correlation-helper (:x values) (:y values) tag1-id tag2-id)))))

(defn compute-and-save-score [vectorize-fn value-type user-id tag1-id tag2-id]
  (let [result  (compute-correlation vectorize-fn user-id tag1-id tag2-id)
        score   (:score result)
        n       (:n result)]
    (when score
      (db/score-update-or-create user-id tag1-id tag2-id score n "tag" "tag" value-type))))

(def compute-and-save-cor
  "This will produce a function with signature [user-id tag1-id tag2-id], which is what iterate-on-tag-pairs expects."
  (partial compute-and-save-score vectorize-in-intervals-cor db/COR_VALUE_TYPE))

(def compute-and-save-trigger
  "This will produce a function with signature [user-id tag1-id tag2-id], which is what iterate-on-tag-pairs expects."
  (partial compute-and-save-score vectorize-in-intervals-trigger db/TRIGGER_VALUE_TYPE))

(defn remove-noise-pairs [all-pairs noise-pairs]
  (remove #(contains? (set noise-pairs) %) all-pairs))

(defn non-noise-pairs [user-id min-n]
  (-> (db/pairs-with-min-n user-id :min-n min-n)
      (remove-noise-pairs (db/noise-pairs user-id))))

(defn iterate-on-tag-pairs [user-id compute-score & {:keys [min-n type-of] :or {min-n MIN-N type-of ""}}]
  "Call a scoring function with signature [user-id tag1-id tag2-id] on all tag pairs, which are not marked as noise."
  (let [pairs (non-noise-pairs user-id min-n)]
    (println* "computing" (count pairs) type-of "correlations")
    (doseq [pair pairs]
      (when @keep-running
        (print* ".") (flush)
        (compute-score user-id (first pair) (second pair))))))

(defn update-user [user-id & {:keys [log-file-name start-time stop-time
                                     max-epoch state min-n
                                     interval-size-ms alpha max-iter-new-cluster
                                     silent]
                              :or {log-file-name const/DEFAULT-LOG-FILE
                                   start-time nil
                                   stop-time nil
                                   max-epoch MAX-EPOCH
                                   state (atom {})
                                   min-n MIN-N
                                   interval-size-ms const/DAY
                                   alpha α
                                   max-iter-new-cluster MAX-ITER-NEW-CLUSTER
                                   silent false}}]

  (reset! SILENT silent)

  (log log-file-name
       (str "USER " user-id ": connect to database\n"))
  (db/connect)

  (db/cluster-run-delete-all-by-user user-id)
  (if (> (db/series-count user-id) 10)
      (do
        (log log-file-name
             (str "refresh-intervals-for-user " user-id))
        (refresh-intervals-for-user user-id
                                       :start-time start-time
                                       :stop-time stop-time
                                       :max-epoch max-epoch
                                       :min-n min-n
                                       :interval-size-ms interval-size-ms
                                       :alpha alpha
                                       :max-iter-new-cluster max-iter-new-cluster)
        (when @keep-running
          (log log-file-name
               (str "compute and save correlations " user-id " " :min-n ": " min-n))
          (db/score-delete-old-unmarked user-id)
          (iterate-on-tag-pairs user-id compute-and-save-cor :min-n min-n :type-of "standard")

          ; TODO: Remove the following line after the analytics job has run at least once on production.
          ; Feb 15, 2015 -- David Beckwith.  There's no harm in leaving it in until the next deployment.
          (db/score-delete-user-value-type user-id db/MIPSS_VALUE_TYPE)

          (log log-file-name
               (str "compute and save triggers " user-id " " :min-n ": " min-n))
          (iterate-on-tag-pairs user-id compute-and-save-trigger :min-n min-n :type-of "trigger"))

        (log log-file-name
             (str "finished update-user " user-id " keep-running: " @keep-running)))
      (println "Skipping user" user-id "because there is/are only" (db/series-count user-id) "data points.")))


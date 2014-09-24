(ns us.wearecurio.analytics.rest
  (:require [clojure.data.json :as json]
            [clj-time.coerce :as tr]
            [org.httpkit.server :as hk]
            [korma.core :as kc]
            [net.cgrand.moustache :as mo]
            [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.interval :as iv]))

(defn tag-list [user-id]
  (kc/select db/analytics_time_series
             (kc/where {:user_id user-id})
             (kc/fields :tag_id)
             (kc/fields :description)
             (kc/modifier "DISTINCT")))

(defn tag-list-json [user-id]
  (json/write-str (tag-list user-id)))

(defn date-list-json [user-id start-date stop-date]
  (json/write-str (iv/data-series-range-with-min-n 10 86400000 start-date stop-date user-id)))

(defn amount-list-base [user-id]
  (-> (kc/select* db/analytics_time_series)
      (kc/fields :date)
      (kc/fields :amount)
      (kc/where {:user_id user-id})
      (kc/order :date)))

(defn -amount-list
  ([user-id]
   (-> (amount-list-base user-id)
       (kc/select)))
  ([user-id tag-id]
  (-> (amount-list-base user-id)
      (kc/where {:tag_id tag-id})
      (kc/select)))
  ([user-id tag-id start-date stop-date]
  (-> (amount-list-base user-id)
      (kc/where {:tag_id tag-id})
      (kc/where (and (>= :date (tr/to-sql-time start-date))
                     (<  :date (tr/to-sql-time stop-date))))
      (kc/select))))

(defn amount-list [& args]
  (map #(update-in % [:date] db/rescale-time 86400000)
       (apply -amount-list args)))

(defn amount-list-json [& args]
  (json/write-str (apply amount-list args)))

(defn cluster-list-json [user-id interval-size-ms]
  (json/write-str (db/cluster-load-latest user-id interval-size-ms)))

; ----- Server -----

(def json-resp
  {:status 200 
   :headers {"Content-type" "application/json"}
   :body ""})

(defn integer [s]
    (try (Integer/parseInt s) (catch Exception e)))

(def routes
  (mo/app
    ["tags" [user-id integer]]
              (fn [req] (assoc json-resp :body (tag-list-json user-id)))
    ["dates" [user-id integer]
             [[_ start-year start-month start-day] #"(\d{4})-(\d{2})-(\d{2})"]
             [[_ stop-year stop-month stop-day] #"(\d{4})-(\d{2})-(\d{2})"] ]
              (fn [req]
                (let [start-time (apply db/sql-time (map integer (list start-year start-month start-day)))
                      stop-time (apply db/sql-time (map integer (list stop-year stop-month stop-day)))]
                  (assoc json-resp :body
                         (date-list-json user-id start-time stop-time))))

    ["amounts" [user-id integer]]
              (fn [req] (assoc json-resp :body (amount-list-json user-id)))

    ["amounts" [user-id integer] [tag-id integer]
                 [[_ start-year start-month start-day] #"(\d{4})-(\d{2})-(\d{2})"]
                 [[_ stop-year stop-month stop-day] #"(\d{4})-(\d{2})-(\d{2})"] ]
              (fn [req]
                (let [start-time (apply db/sql-time (map integer (list start-year start-month start-day)))
                      stop-time (apply db/sql-time (map integer (list stop-year stop-month stop-day)))]
                (assoc json-resp :body (amount-list-json user-id tag-id start-time stop-time))))


    ["clusters" [user-id integer] [interval-size-ms integer]]
      (fn [req] (assoc json-resp :body (cluster-list-json user-id interval-size-ms)))))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (reset! server (hk/run-server #'routes {:port 8080})))


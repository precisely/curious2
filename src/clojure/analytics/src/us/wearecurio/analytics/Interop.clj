(ns us.wearecurio.analytics.Interop
  (:require [us.wearecurio.analytics.core :as core]
            [us.wearecurio.analytics.database :as db])
  (:gen-class
   :name "us.wearecurio.analytics.Interop"
   :methods [#^{:static true} [updateUser [String Long String] void]]))

(defn -updateUser [environment user-id log-file-name]
  "The Java wrapper for us.wearecurio.analytics/update-user. The argument \"environment\" is either nil or a string with the possible values: DEVELOPMENT, TEST, or PRODUCTION.  See us.wearecurio.analytics.database/connect for details."
  (let [params (db/connection-params environment)]
    (spit log-file-name "connect to database\n" :append true)
    (db/connect params)
    (spit log-file-name (str "core/update-user " user-id "\n") :append true)
    (core/update-user user-id log-file-name)))

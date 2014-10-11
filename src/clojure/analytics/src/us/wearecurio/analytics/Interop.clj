(ns us.wearecurio.analytics.Interop
  (:require [us.wearecurio.analytics.core :as core]
            [us.wearecurio.analytics.database :as db])
  (:gen-class
   :name "us.wearecurio.analytics.Interop"
   :methods [#^{:static true} [updateUser [String Long] void]]))

(defn -updateUser [environment user-id]
  "The Java wrapper for us.wearecurio.analytics/update-user. The argument \"environment\" is either nil or a string with the possible values: DEVELOPMENT, TEST, or PRODUCTION.  See us.wearecurio.analytics.database/connect for details."
  (let [params (db/connection-params environment)]
    (db/connect params)
    (core/update-user user-id)))

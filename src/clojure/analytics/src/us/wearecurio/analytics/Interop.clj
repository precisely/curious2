(ns us.wearecurio.analytics.Interop
	(:require [us.wearecurio.analytics.core :as core]
	          [us.wearecurio.analytics.database :as db])
	(:gen-class
	 :name "us.wearecurio.analytics.Interop"
	 :methods [#^{:static true} [updateAllUsers [String] void]]))

(defn -updateAllUsers [environment]
	"The Java wrapper for us.wearecurio.analytics/update-all-users. The argument \"environment\" is either nil or a string with the possible values: DEVELOPMENT, TEST, or PRODUCTION.  See us.wearecurio.analytics.database/connect for details."
	(let [params (db/connection-params environment)]
	  (db/connect params)
	  (core/update-all-users)))

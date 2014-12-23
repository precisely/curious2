{ :user {:plugins [[lein-midje "3.1.1"]]}
    :dev  {:env {:env     "dev"
                 :db-name "tlb_dev"
                 :db-user "root"
                 :db-pass nil
                 :key "replace with local key in LocalConfig.groovy"
                 }}
    :production {:env {:env     "dev"
                       :db-name "tlb_dev"
                       :db-user "curious"
                       :db-pass nil
                       :key "replace with local key in LocalConfig.groovy"
                       }}
	:test {:dependencies [[org.clojure/tools.nrepl "0.2.3"]]
	           :env {:env     "test"
                     :db-name "tlb_test"
                     :db-user "root"
                     :db-pass "abc123"
                     :key "replace with local key in LocalConfig.groovy"
                     }}}

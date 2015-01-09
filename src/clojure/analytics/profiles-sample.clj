{
  :dev  {:env {:env     "dev"
               :web-secret "replace with local key in LocalConfig.groovy"
               :web-url "http://localhost:8080"
               :web-next-path "/analyticsTask/runNext"
               :web-done-path "/analyticsTask/done"
               :db-host "127.0.0.1"
               :db-port "3306"
               :db-name "tlb_dev"
               :db-user "root"
               :db-pass "abc123"}}
  :test {:dependencies [[org.clojure/tools.nrepl "0.2.3"]]
         :env {:env     "test"
               :web-secret "replace with local key in LocalConfig.groovy"
               :web-url "http://localhost:8080"
               :web-next-path "/analyticsTask/runNext"
               :web-done-path "/analyticsTask/done"
               :db-host "127.0.0.1"
               :db-schema-from "tlb_dev"
               :db-port "3306"
               :db-name "tlb_test"
               :db-user "root"
               :db-pass "abc123"}}
  :production {
         :env {:env     "production"
               :web-secret "replace with local key in LocalConfig.groovy"
               :web-url "http://localhost:8080"
               :web-next-path "/analyticsTask/runNext"
               :web-done-path "/analyticsTask/done"
               :db-host "127.0.0.1"
               :db-port "3306"
               :db-name "tlb_dev"
               :db-user "root"
               :db-pass "abc123"}}}

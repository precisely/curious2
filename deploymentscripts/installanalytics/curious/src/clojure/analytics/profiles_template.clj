{
  :dev  {:env {:env     "dev"
               :web-secret "ADMINKEY"
               :web-url "http://curiouswebapp:8080"
               :web-next-path "/analyticsTask/runNext"
               :web-done-path "/analyticsTask/done"
               :db-host "curiousdb"
               :db-port "3306"
               :db-name "DBNAME_dev"
               :db-user "curious"
               :db-pass "734qf7q35"}}
  :test {:dependencies [[org.clojure/tools.nrepl "0.2.3"]]
         :env {:env     "test"
               :web-secret "ADMINKEY"
               :web-url "http://curiouswebapp:8080"
               :web-next-path "/analyticsTask/runNext"
               :web-done-path "/analyticsTask/done"
               :db-host "curiousdb"
               :db-schema-from "DBNAME_dev"
               :db-port "3306"
               :db-name "DBNAME_test"
               :db-user "curious"
               :db-pass "734qf7q35"}}
  :production {
         :env {:env     "production"
               :web-secret "ADMINKEY"
               :web-url "http://curiouswebapp:8080"
               :web-next-path "/analyticsTask/runNext"
               :web-done-path "/analyticsTask/done"
               :db-host "curiousdb"
               :db-port "3306"
               :db-name "DBNAME"
               :db-user "DBUSERNAME"
               :db-pass "DBPASSWORD"}}}

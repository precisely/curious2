(defproject analytics "0.1.2"
  :description "Analytics for Curious, Inc."
  :url "http://precise.ly"
  :plugins [[lein-environ "0.5.0"]]
  ;:aot [us.wearecurio.analytics.Interop]
  :main us.wearecurio.analytics.core
  ; Adjust your system environment variables by placing them
  ; in "./profiles.clj".  The values in your profiles.clj will
  ; override those values defined here in the :profiles key
  ; value.  For example, to use the dev profile values,
  ;
  ;   $ lein with-profile dev repl
  ;
  ; :profiles {:dev {:env {:db "tlb_test"
  ;                        :user "root"
  ;                        :pass nil}}}
  :source-paths ["dev" "src" "test"]
  :dependencies [; misc.
                 [http-kit.fake "0.2.1"]
                 [environ "0.5.0"]
                 [clj-time "0.7.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 ; numeric stuff
                 [incanter/incanter-core "1.5.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 ; database
                 [korma "0.4.0"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [org.clojure/java.jdbc "0.3.3"]
                 ; web stuff
                 [net.cgrand/moustache "1.1.0"]
                 [http-kit "2.1.16"]
                 [cheshire "5.3.1"]
                 [org.clojure/data.json "0.2.5"]
                 [ring/ring-core "1.3.1"]])

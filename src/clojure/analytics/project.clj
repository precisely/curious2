(defproject analytics "0.1.0"
	:description "Analytics for Curious, Inc."
	:url "http://wearecurio.us"
	:plugins [[lein-environ "0.5.0"]]
	:aot [us.wearecurio.analytics.Interop]

	; Adjust your system environment variables by placing them
	; in "./profiles.clj".  The values in your profiles.clj will
	; override those values defined here in the :profiles key
	; value.  For example, to use the dev profile values,
	;
	;   $ lein with-profile dev repl
	;
	:profiles {:dev {:env {:db "tlb_test"
									       :user "root"
												 :pass nil}}}
	:dependencies [[environ "0.5.0"]
	               [org.clojure/clojure "1.5.1"]
								 [org.clojure/math.numeric-tower "0.0.4"]
								 [org.clojure/java.jdbc "0.3.3"]
								 [midje "1.6.3"]
								 [mysql/mysql-connector-java "5.1.25"]
								 [clj-time "0.7.0"]
								 [korma "0.3.1"]])

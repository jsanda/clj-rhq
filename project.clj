(defproject rhq-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write"
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [lein-difftest "1.3.1"]]  
  :dependencies [[org.rhq/rhq-core-plugin-container "4.0.0-SNAPSHOT"]
		 [org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 ;[oswego-concurrent/concurrent "1.3.4-jboss-update1"]
 		 ;[log4j/log4j "(1.0,)"]
		 ;[org.rhq/remote-client-deps "4.0.0-SNAPSHOT" :type "pom"]
                 [org.rhq/rhq-core-plugin-api "4.0.0-SNAPSHOT"]
		 [org.rhq/clj-test "4.0.0-SNAPSHOT"]]
  ;:hooks [leiningen.hooks.difftest]
  ;:aot [rhq.plugin]
  :source-path "src/main/clj"
  :target-dir "target")

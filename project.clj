(defproject rhq-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write"
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [lein-difftest "1.3.1"]]  
  :dependencies [[org.rhq/rhq-core-plugin-container "4.0.0-SNAPSHOT"]
		 [org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.rhq/rhq-core-plugin-api "4.0.0-SNAPSHOT"]
		 [org.rhq/clj-test "4.0.0-SNAPSHOT"]
		 [commons-logging "1.1.0.jboss"]]
  :source-path "src/main/clj"
  :target-dir "target"
  :native-path "lib/native")

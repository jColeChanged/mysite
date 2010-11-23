(defproject mysite "1.0.0-SNAPSHOT"
  :description "A portfolio site. Hopefully it will get me a job."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [compojure "0.5.3"]
		 [ring/ring-jetty-adapter "0.3.1"]
		 [enlive "1.0.0-SNAPSHOT"]
		 [pour "0.1.0"]]
  :dev-dependencies [[appengine-magic "0.3.0-SNAPSHOT"]
		     [swank-clojure "1.2.1"]]
  :namespaces [mysite.app_servlet])
(ns mysite.core
  (:use compojure.core)
  (:require [net.cgrand.enlive-html :as html])
  (:require [appengine-magic.core :as ae]))

;; Templates
(html/deftemplate add-project "mysite/projects/add.html" [] [] [])
(defn show-project
  [request]
  (str request))

(defroutes mysite-app-handler
  (GET "/projects/add/" [] (add-project))
  (POST "/projects/add/" [] show-project)
  (GET "*" req
       {:status 200
	:headers {"Content-Type" "text/plain"}
	:body "making changes live*"}))


(ae/def-appengine-app mysite-app #'mysite-app-handler)

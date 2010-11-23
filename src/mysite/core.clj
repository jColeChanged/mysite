(ns mysite.core
  (:use compojure.core, ring.middleware.keyword-params, ring.middleware.params,
	ring.util.response, pour.core, pour.validators)
  (:require [net.cgrand.enlive-html :as html])
  (:require [appengine-magic.core :as ae]))

;; Forms
(defform add-project-form
  :title [required "Title is required."]
  :pitch [required "Pitch is required."]
  :details [required "Details are required."]
  :priority [required "A priority is required."
	     an-integer "The priority must be an integer."])

(defn set-input
  [id form]
  (html/set-attr :value (str (id (:values form)))))

(defn set-error
  [id form]
  (html/html-content (id (:errors form))))
  
;; Templates
(html/deftemplate add-project-template "mysite/projects/add.html"
  [form-data]
  [:div#title :input] (set-input :title form-data)
  [:div#title :p] (set-error :title form-data)
  [:div#pitch :input] (set-input :pitch form-data)
  [:div#pitch :p] (set-error :pitch form-data)
  [:div#details :textarea] (html/html-content (:details (:values form-data)))
  [:div#details :p] (set-error :details form-data)
  [:div#priority :input] (set-input :priority form-data)
  [:div#priority :p] (set-error :priority form-data))

(defn show-project
  [params]
  (let [form-data (add-project-form params)]
    (if (empty? (:errors form-data))
      (redirect "/projects/list/")
      (add-project-template form-data))))

(defroutes mysite-app-handler
  (GET "/projects/add/" [] (add-project-template 0))
  (POST "/projects/add/" {params :params} (show-project params))
  (GET "*" req
       {:status 200
	:headers {"Content-Type" "text/plain"}
	:body "making changes live*"}))

(def mysite-app-handler
     (-> mysite-app-handler
	 wrap-params
	 wrap-keyword-params
	 wrap-params))

(ae/def-appengine-app mysite-app #'mysite-app-handler)

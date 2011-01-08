(ns mysite.core
  (:use compojure.core
	ring.middleware.keyword-params
	ring.middleware.params
	ring.util.response
	pour.core
	pour.validators)
  (:require [compojure.route :as route]
	    [net.cgrand.enlive-html :as html]
	    [appengine-magic.core :as ae]
	    [appengine-magic.services.datastore :as ds]
	    [appengine-magic.services.user :as au]))

(defn require-admin
  [func]
  (fn [& args]
    (if (and (au/user-logged-in?) (au/user-admin?))
      (apply func args)
      (redirect (au/login-url)))))

;; Models ==================================================================
(ds/defentity Project [^:key title, url, pitch, details, priority])

(defn add-project-to-store
  [data]
  (let [project (Project. (:title data) (:url data) (:pitch data)
			  (:details data) (:priority data))]
    (ds/save! project)))

;; Forms ===================================================================
(defform add-project-form
  :title [required "Title is required."]
  :url [required "A URL is required."]
  :pitch [required "Pitch is required."]
  :details [required "Details are required."]
  :priority [required "A priority is required."
	     an-integer "The priority must be an integer."])

;; Templates ===============================================================
(defn set-input
  [id form]
  (html/set-attr :value (str (id (:values form)))))

(defn set-error
  [id form]
  (html/html-content (id (:errors form))))

(html/deftemplate view-homepage-template "mysite/projects/home.html" [] [] [])

(html/deftemplate add-project-template "mysite/projects/add.html"
  [form-data]
  [:div#title :input] (set-input :title form-data)
  [:div#title :p] (set-error :title form-data)
  [:div#url :input] (set-input :url form-data)
  [:div#url :p] (set-error :url form-data)
  [:div#pitch :input] (set-input :pitch form-data)
  [:div#pitch :p] (set-error :pitch form-data)
  [:div#details :textarea] (html/html-content (:details (:values form-data)))
  [:div#details :p] (set-error :details form-data)
  [:div#priority :input] (set-input :priority form-data)
  [:div#priority :p] (set-error :priority form-data))

(def *project-selector* [[:.project html/first-child]])

(html/defsnippet project-snippet "mysite/projects/view.html" *project-selector*
  [project]
  [:h2] (html/html-content (.title project))
  [:a] (html/set-attr :href (.url project))
  [:p.pitch] (html/html-content (.pitch project))
  [:div.details] (html/html-content (.details project)))

(html/deftemplate view-projects-template "mysite/projects/view.html"
  [projects]
  [:.projects] (html/content (map project-snippet projects)))

;; Views ===================================================================


(defn view-homepage
  []
  (view-homepage-template))

(defn add-project
  [params]
  (let [form-data (add-project-form params)]
    (if (empty? (:errors form-data))
      (do
	(add-project-to-store (:values form-data))
	(redirect "/projects/view/"))
      (add-project-template form-data))))
(def add-project-view (require-admin add-project))

(defn view-projects
  []
  (view-projects-template (ds/query :kind Project)))

;; Routes ==================================================================
(defroutes admin-routes
  (GET "/projects/add/" [] (add-project-view {}))
  (POST "/projects/add/" {params :params} (add-project-view params)))
 
(defroutes mysite-app-handler
  admin-routes
  (GET "/" [] (view-homepage))
  (GET "/projects/view/" [] (view-projects))
  (route/not-found "Page not found."))

(def mysite-app-handler
     (-> mysite-app-handler
	 wrap-params
	 wrap-keyword-params
	 wrap-params))

(ae/def-appengine-app mysite-app #'mysite-app-handler)
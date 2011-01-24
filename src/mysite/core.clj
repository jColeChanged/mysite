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
(ds/defentity ExtendableGame [^:key name, url, embed])

(defn add-project-to-store
  [data]
  (ds/save! (Project. (:title data)
		      (:url data)
		      (:pitch data)
		      (:details data)
		      (:priority data))))

(defn add-extendable-game-to-store
  [data]
  (ds/save! (ExtendableGame. (:name data)
			     (:url data)
			     (:embed data))))
;; Forms ===================================================================
(defform add-project-form
  :title [required "Title is required."]
  :url [required "A URL is required."]
  :pitch [required "Pitch is required."]
  :details [required "Details are required."]
  :priority [required "A priority is required."
	     an-integer "The priority must be an integer."])

(defform add-extendable-game-form
  :name [required "Name is required."]
  :url [required "URL is required."]
  :embed [required "Must supply text to embed"])
;; Templates ===============================================================
(defmacro maybe-substitute
  ([expr] `(if-let [x# ~expr] (html/substitute x#) identity))
  ([expr & exprs] `(maybe-substitute (or ~expr ~@exprs))))

(defmacro maybe-content
  ([expr] `(if-let [x# ~expr] (html/content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

(defmacro maybe-substitute
  ([expr] `(if-let [x# ~expr] (html/substitute x#) identity))
  ([expr & exprs] `(maybe-substitute (or ~expr ~@exprs))))

(defmacro maybe-html-content
  ([expr] `(if-let [x# ~expr] (html/html-content x#) identity))
  ([expr & exprs] `(maybe-html-content (or ~expr ~@exprs))))

(html/defsnippet footer "mysite/resources/footer.html" [:script]
  []
  [] [])

(defn set-input
  [id form]
  (html/set-attr :value (str (id (:values form)))))

(defn set-error
  [id form]
  (html/html-content (id (:errors form))))

(html/deftemplate view-homepage-template "mysite/projects/home.html"
  []
  [:#footer] (html/content (footer)))

(html/deftemplate add-project-template "mysite/projects/add.html"
  [form-data]
  [:div#title :input] (set-input :title form-data)
  [:div#url :input] (set-input :url form-data)
  [:div#pitch :input] (set-input :pitch form-data)
  [:div#details :textarea] (html/html-content (:details (:values form-data)))
  [:div#priority :input] (set-input :priority form-data))

(def *project-selector* [[:.project html/first-child]])
(html/defsnippet project-snippet "mysite/projects/view.html" *project-selector*
  [project]
  [:h2 :em] (html/html-content (.title project))
  [:a] (html/set-attr :href (.url project))
  [:p.pitch] (html/html-content (.pitch project))
  [:div.details] (html/html-content (.details project)))

(def *not-free-selector* [:ul#notfree :li])
(html/defsnippet not-free-snippet "mysite/extendable/home.html" *not-free-selector*
  [game]
  [:a] (html/do->
	(html/content (.name game))
	(html/set-attr :href (str "/extendable/notfree/?name=" (.name game)))))
	

(html/deftemplate view-projects-template "mysite/projects/view.html"
  [projects]
  [:.projects] (html/content (map project-snippet projects))
  [:#footer] (html/content (footer)))


(html/deftemplate extendable-template "mysite/extendable/home.html"
  [{:keys [body games]}]
  [:#contentcolumn :.innertube] (maybe-html-content
				 (if (map? body) (.embed body)))
  [:#notfree] (html/content (map not-free-snippet games))
  [:#footer] (html/content (footer)))

(html/deftemplate add-extendable-game-template "mysite/extendable/add.html"
  [game]
  [:#name] (set-input :name game)
  [:#url] (set-input :url game)
  [:#embed] (html/html-content (:embed (:values game))))
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

(defn add-extendable-game
  [params]
  (let [form-data (add-extendable-game-form params)]
    (if (empty? (:errors form-data))
      (do
	(add-extendable-game-to-store (:values form-data))
	(redirect "/extendable/"))
      (add-extendable-game-template form-data))))
(def add-extendable-game-view (require-admin add-extendable-game))

(defn view-extendable-home
  []
  (extendable-template
   {:body nil
    :games (ds/query :kind ExtendableGame)}))

(defn not-free-game-view
  [name]
  (let [game (first (ds/query :key name :kind ExtendableGame))]
    (if (nil? game)
      404
      (extendable-template
       {:body game
	:games (ds/query :kind ExtendableGame)}))))

;; Routes ==================================================================
(defroutes extendable-admin-routes
  (ANY "/extendable/add/" {params :params}
       (add-extendable-game-view params)))

(defroutes extendable-routes
  (GET "/extendable/" [] (view-extendable-home))
  (GET "/extendable/notfree/" {{name "name"} :params} (not-free-game-view name))
  extendable-admin-routes)

(defroutes project-admin-routes
  (GET "/projects/add/" [] (add-project-view {}))
  (POST "/projects/add/" {params :params} (add-project-view params)))

(defroutes project-routes
  (GET "/projects/view/" [] (view-projects))
  project-admin-routes)

(defroutes mysite-app-handler
  (GET "/" [] (view-homepage))
  project-routes
  extendable-routes
  (route/not-found 404))

(def mysite-app-handler
     (-> mysite-app-handler
	 wrap-params
	 wrap-keyword-params
	 wrap-params))

(ae/def-appengine-app mysite-app #'mysite-app-handler)
(ns socialauto.core)
  (require '[etaoin.api :as e])
  (require '[etaoin.keys :as k])

  (require '[socialauto.scraper :as scraper])

(def driver (e/firefox))
(def phone-number (System/getenv "PHONE_NUMBER"))
(def minds-username (System/getenv "MINDS_USERNAME"))
(def minds-password (System/getenv "MINDS_PASSWORD"))
(def twitter-email (System/getenv "TWITTER_EMAIL"))
(def twitter-password (System/getenv "TWITTER_PASSWORD"))
(def ivan-password (System/getenv "IVAN_PASSWORD"))
(def ivan-username(System/getenv "IVAN_USERNAME"))
(def ivan-url (System/getenv "IVAN_URL"))
(defn now [] (java.util.Date.))

(defn reload [browser]
  (e/quit browser)
  (require 'socialauto.core :reload-all))

(defn fill-number-next []
    (e/fill driver {:tag :input :name :text} phone-number)
    (e/click driver {:data-testid :ocfEnterTextNextButton}))

(defn tweet-from-url [url]
  (as-> url u
    (vector u, (scraper/first-thirty-words-from-url u))
      (clojure.string/join " " u))
  )

;; TODO break down into login-to-twitter and post-to-twitter

(defn login-to-twitter []
  (e/go driver "https://twitter.com/login")
  (e/wait-visible driver {:tag :input :name :text})
  (e/fill driver {:tag :input :name :text} twitter-email)
  (e/click driver {:role :button :index 2})
  (e/wait-visible driver {:tag :input})
  (if (e/exists? driver {:data-testid :ocfEnterTextNextButton})
    (fill-number-next)
      )
  (e/wait-visible driver {:tag :input})
  (e/fill driver {:tag :input :name :password} twitter-password)
  (e/click driver {:data-testid :LoginForm_Login_Button})
  )

(defn tweet [text]
  (e/wait-visible driver {:data-testid :tweetTextarea_0RichTextInputContainer})
  (e/click driver {:tag :a :aria-label :Tweet})
  (e/wait-visible driver {:data-testid :tweetTextarea_0})
  (e/fill driver {:data-testid :tweetTextarea_0} text)
  (e/click driver {:data-testid :tweetButton})
  )

(defn post-to-twitter [text]
  (login-to-twitter)
  (tweet text)
  )

(defn login-to-minds []

  (e/go driver "https://www.minds.com/beerscb/subscriptions")
  (e/click driver {:class "m-button m-button--grey m-button--small"})
  (e/wait-visible driver {:id "username"})
  (e/fill driver {:id "username"} minds-username)
  (e/fill driver {:id "password"} minds-password)
  (e/click driver {:class "m-login__button--login"}))

(defn mind [text]
  (e/wait-visible driver {:class "m-icon__assetsFile ng-star-inserted"})
 (e/click driver {:class "m-icon__assetsFile ng-star-inserted"})
  (e/wait-visible driver {:data-cy "composer-textarea"})
  (e/fill driver {:data-cy "composer-textarea"} text)
  (e/wait-visible driver {:tag :button :class "m-button m-button--blue m-button--small m-button--dropdown":type "submit"})
  (Thread/sleep 10000)
 (e/click-single driver {:tag :button :class "m-button m-button--blue m-button--small m-button--dropdown":type "submit"})
  )

(defn post-to-minds [text]
 (login-to-minds)
  (mind text))

(defn tweet-blog [url]
  (post-to-twitter (tweet-from-url url)))
(defn mind-blog [url]
  (post-to-minds (tweet-from-url url)))

(defn get-latest-substack-url []
  (e/go driver "https://calebbeers.substack.com/")
  (e/click driver {:tag :button :class "button maybe-later"})
  (e/click-el driver
    (first
      (e/query-all driver {:class "post-preview portable-archive-post"})))
  (e/get-url driver))

(defn promote-latest-substack []
  (let [substack-url (get-latest-substack-url)]
    (tweet-blog substack-url)
    (Thread/sleep 2000)
    (reload driver)
    (mind-blog substack-url)))

(defn tweet-and-mind [url]
    (post-to-twitter url)
    (Thread/sleep 2000)
    (reload driver)
    (post-to-minds url))

(defn any-offers? [count]

  (Thread/sleep (* 1000 60) )
  (let [text (e/get-element-text driver {:tag :h5 :id "offers-description"})]
    (not (clojure.string/includes? text "Showing 0 job") )))

      ;; (e/js-execute driver "document.querySelector('[id*=\"Claim\"]').click();")

(defn claim [browser]
(println "Attempting to claim...")
  (let [id (e/js-execute driver "return document.querySelector('[id*=\"Claim-\"]').id" )]
  (println "The id is " id)
;; (e/js-execute driver (str "document.getElementById("
;;                           id
;;                           ").click();")))
(e/click driver {:id id})
  (Thread/sleep (* 1000 10) )
(throw (Exception. "Grabbed one!"))
  ))

(defn refresh-until-offers-available [count]
  (println (str "None yet - have tried " count " times so far."))
  (println (str "Tried at: " (now) "\n"))

  (e/wait-visible driver {:id "MoreOffersButton"})
  (e/click driver {:id "MoreOffersButton"})
    (if (any-offers? count)
      (claim driver)
      (recur (+ count 1)))
      )

(defn login-to-portal []
  (reload driver)
  (e/go driver ivan-url)
  (e/wait-visible driver {:class "modal-content background-customizable modal-content-desktop visible-md visible-lg"} {:tag :input :id "signInFormUsername" :type :text})
;; I think I need a let binding here to let the js-execute do its thing.
(e/js-execute driver (str "document.getElementById(arguments[0].id).value = '" ivan-username "';" ) {:tag :input :id :signInFormUsername} )
(e/js-execute driver (str "document.getElementById(arguments[0].id).value = '" ivan-password "';" ) {:tag :input :id :signInFormPassword} )
  ;; Just need to get it submit the form because it IS already changing the form vals
(e/js-execute driver "document.getElementsByName(\"signInSubmitButton\")[0].click();")
(e/wait-visible driver {:tag :a :href "/offers"})
(e/click driver {:tag :a :href "/offers"})
(e/wait-visible driver {:id "MoreOffersButton"} [:timeout 180])
(try
  (refresh-until-offers-available 0)
     (catch Exception e (login-to-portal)))
  )

(defn try-portal [times]
(loop [tries times]
  (println (str times " iterations left"))
  (when (try
          (login-to-portal)
          false ; so 'when' is false, whatever 'might-throw-exception' returned
          (catch Exception e
            (pos? tries)))
    (recur (dec tries)))))

;; (defn -main [& args]
;;   (tweet-blog (first args))
;;   (Thread/sleep 2000)
;;   (reload driver)
;;   (mind-blog (first args)))

;; TODO make a wrapper that takes an action (fill, wait, click-single, etc.), a browser, and a tuple of selectors and then performs that action using that browser on that tuple *after* waiting for the element in question to appear and then an addition 1 second.
;; (defn wait-then [action browser tuple])

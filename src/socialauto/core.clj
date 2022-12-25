(ns socialauto.core
  (:require [etaoin.api :as e])
  (:require [etaoin.keys :as k])
  )
  (require '[socialauto.scraper :as scraper])

(def driver (e/chrome))
(def phone-number (System/getenv "PHONE_NUMBER"))
(def minds-username (System/getenv "MINDS_USERNAME"))
(def minds-password (System/getenv "MINDS_PASSWORD"))
(def twitter-email (System/getenv "TWITTER_EMAIL"))
(def twitter-password (System/getenv "TWITTER_PASSWORD"))

(defn fill-number-next []
    (e/fill driver {:tag :input :name :text} phone-number)
    (e/click driver {:data-testid :ocfEnterTextNextButton}))

(defn tweet-from-url [url]
  (as-> url u
    (vector u, (scraper/first-thirty-words-from-url u))
      (clojure.string/join " " u))
  )

(defn minds []

  (e/go driver "https://www.minds.com/beerscb/subscriptions")
  (e/click driver {:class "m-button m-button--grey m-button--small"})
  (e/wait-visible driver {:id "username"})
  (e/fill driver {:id "username"} minds-username)
  (e/fill driver {:id "password"} minds-password)
  (e/click driver {:class "m-login__button--login"}))

(defn post-to-twitter [text]
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
  (e/wait-visible driver {:data-testid :tweetTextarea_0RichTextInputContainer})
  (e/click driver {:tag :a :aria-label :Tweet})
  (e/wait-visible driver {:data-testid :tweetTextarea_0})
  (e/fill driver {:data-testid :tweetTextarea_0} text)
  (e/click driver {:data-testid :tweetButton})
  )

;; (e/click driver {:class "minds-avatar"})

;; (e/wait-visible driver {:class "m-button m-button--grey m-button--medium"})

;; (e/click driver {:class "m-button m-button--grey m-button--medium"})

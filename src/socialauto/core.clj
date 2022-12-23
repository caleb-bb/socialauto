(ns socialauto.core
  (:require [etaoin.api :as e])
  (:require [etaoin.keys :as k]))

(def driver (e/chrome))

(defn fill-number-next []
    (e/fill driver {:tag :input :name :text} "9378561495")
    (e/click driver {:data-testid :ocfEnterTextNextButton}))

(defn minds []

  (e/go driver "https://www.minds.com/beerscb/subscriptions")
  (e/click driver {:class "m-button m-button--grey m-button--small"})
  (e/wait-visible driver {:id "username"})
  (e/fill driver {:id "username"} "beerscb")
  (e/fill driver {:id "password"} "Pickles$012")
  (e/click driver {:class "m-login__button--login"}))

(defn post-to-twitter [text]
  (e/go driver "https://twitter.com/login")
  (e/wait-visible driver {:tag :input :name :text})
  (e/fill driver {:tag :input :name :text} "philosophy.beard@gmail.com")
  (e/click driver {:role :button :index 2})
  (e/wait-visible driver {:tag :input})
  (if (e/exists? driver {:data-testid :ocfEnterTextNextButton})
    (fill-number-next)
      )
  (e/wait-visible driver {:tag :input})
  (e/fill driver {:tag :input :name :password} "Pickles$006")
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

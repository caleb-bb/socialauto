(ns socialauto.scraper
(:require [clj-http.client :as client]))
(require '[clojure.string :as string])
(require '[hickory.select :as s])

(use 'hickory.core)

(def commonwords #{"" "the" "of" "and" "a" "to" "in" "is" "you" "that" "it" "he" "was" "for" "on" "are" "as" "with" "his" "they" "I" "at" "be" "this" "have" "from" "or" "one" "had" "by" "word" "but" "not" "what" "all" "were" "we" "when" "your" "can" "said" "there" "use" "an" "each" "which" "she" "do" "how" "their" "if" "will" "up" "other" "about" "out" "many" "then" "them" "these" "so" "some" "her" "would" "make" "like" "him" "into" "time" "has" "look" "two" "more" "write" "go" "see" "number" "no" "way" "could" "people" "my" "than" "first" "water" "been" "call" "who" "oil" "its" "now" "find" "long" "down" "day" "did" "get" "come" "made" "may" "part"})

;This is just for sheer convenience in using the REPL
(defn reload []
  (require 'socialauto.core :reload))

(defn current-date []
  (.format
   (new java.text.SimpleDateFormat "yyyy-MM")
   (java.util.Date.)))

(defn generate-filename [date domain title]
  (string/join "" [date "-"  domain "-" title ".txt"]))

(defn save-article [date domain title text]
  (def filename (generate-filename date domain title))
  (spit filename text))

;NYT stuff

(defn get-html [url]
  (get (client/get url) :body))

(defn quote-string [some-string]
  (string/join [\" some-string \"]))

(defn filter-query [fieldname value-vec]
  (as-> value-vec V
    (map quote-string V)
    (string/join " " V)
    (string/join [fieldname ":(" V ")"])))

(defn nyt-dates [YYYYMMDD-begin YYYYMMDD-end]
  (string/join ["&begin_date=" YYYYMMDD-begin "&end_date=" YYYYMMDD-end]))

(defn nyt-facets [facet]
  (string/join ["facet_fields=" facet]))

(defn nyt-build-query [query-vec]
  (as-> query-vec Q
    (string/join "&" Q)
    (string/replace Q "&&" "&")
    (string/join ["https://api.nytimes.com/svc/search/v2/articlesearch.json?q=" Q "&api-key=wGNUhgyB28zUKs7VIfyy4mjuQm3EPXMN"])))

(defn nyt-get [query]
  (client/get query))

(defn links-from-json [json-response]
  (as-> json-response J
    (get J :body)
    (re-seq #"https[^\"]*" J)))

(defn search-1st [query]
  (-> query
      (nyt-get)
      (links-from-json)
      (first)))

;here ends all the NYT stuff

(defn hickory-this [html]
  (as-hickory (parse html)))

(defn url-to-hickory [url]
  (-> url
      (get-html)
      (hickory-this)))

;;not sure how to implement this yet...
(defn leaves [x]
  (filter (complement coll?)
          (rest (tree-seq coll? identity x))))

(defn clean-text [hickory-struct]
  (cond
    ;note to self: possibly replace (recur (first hickory-struct)) with (map recur hickory-struct)
    (vector? hickory-struct) (recur (first hickory-struct))
    (map? hickory-struct) (recur (get hickory-struct :content))
    (string? hickory-struct) hickory-struct
    :else  ""))

(defn all-clean-text [hickory-struct separator]
  (->> hickory-struct
       (map clean-text)
       (string/join separator)))

(defmacro retrieve-text [hickory-struct identifier value]
  (list s/select
        (list s/descendant
              (list identifier value))
        hickory-struct))

(defn get-tag-p [hickory-struct]
  (->
   (retrieve-text hickory-struct s/tag :p)
   (all-clean-text " ")))

(defn get-tag-span [hickory-struct]
  (->
   (retrieve-text hickory-struct s/tag :span)
   (all-clean-text " ")))

(def func-map {"nytimes.com" get-tag-p
               "hopelutheransunbury.org" get-tag-span
               "thefederalist.com" get-tag-p
               "theguardian.co" get-tag-p
               "substack.com" get-tag-p})

(defn extract-domain [url]
  (->> url
       (re-find #"^(?:.*://)?(?:.*?\.)?([^:/]*?\.[^:/]*).*$")
       (last)))

(defn domain-func [url]
  (let [domain (extract-domain url)]
    (get func-map domain)))

(defn url-to-text [url]
  (let [hickory (url-to-hickory url)
        function (domain-func url)]
    (function hickory)))

(defn get-title-vec [hickory-struct]
  (->
   (retrieve-text hickory-struct s/tag :a)
   (all-clean-text "!!!!!")
   (string/replace #" " "_")
   (string/split #"!!!!!")))

(defn get-domain-vec [domain-name length]
  (->> domain-name
       (repeat length)
       (vec)))

(defn get-filename-vec [date-vec domain-vec title-vec]
  (map generate-filename date-vec domain-vec title-vec))

(defn get-url [item]
  (-> item
      (get :attrs)
      (get :href)))

(defn freq-map [gotten-text]
  (->> gotten-text
       (re-seq #"[\w|’|']*")
       (map string/lower-case)
       (remove commonwords)
       (frequencies)))

(defn n-most-common [mapped-text n]
  (->> mapped-text
       (sort-by val #(compare %2 %1))
       (take n)))

(defn top-ten [mapped-text]
  (n-most-common mapped-text 10))

(defn first-thirty-words-from-text [text]
  (as-> text t
      (string/split t #" ")
      (take 30 t)
      (string/join " " t)
      (string/join [t "..."])))

(defn first-thirty-words-from-url [url]
  (-> url
      (url-to-text)
      (first-thirty-words-from-text)))

;these are just some pre-defined values for use in the REPL when testing/developing
(def url "https://www.nytimes.com/search?dropmab=true&endDate=20200801&query=&sort=best&startDate=20200401")
(def hick-struct (url-to-hickory url))
(def scraped (get-tag-p hick-struct))
(def lynx (map get-url (retrieve-text hick-struct s/tag :a)))
(def nyt-req (nyt-build-query ""))
(def nyt-resp (nyt-get nyt-req))

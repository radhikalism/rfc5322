(ns rfc5322.parser
  (:require
    [clojure.java.io :as io]
    [instaparse.core :as instaparse]
    [taoensso.timbre :as log])
  (:import
    (rfc5322.exception ParserException))
  (:refer-clojure :exclude [parse]))

(defn read-grammar
  [filename]
  (io/input-stream (io/resource filename)))

(defn make-grammar-parser
  [filename]
  (instaparse/parser
    (read-grammar filename)
    :input-format :abnf
    :instaparse.abnf/case-insensitive true))

(defn make-parser
  [mode]
  (case mode
    [:lite] (make-grammar-parser "rfc5322-no-obselete.abnf")
    [:full] (make-grammar-parser "rfc5322.abnf")
    [:lite :utf8] (make-grammar-parser "rfc5322-no-obselete-utf8.abnf")
    [:full :utf8] (make-grammar-parser "rfc5322-utf8.abnf")))

(defn obsolete
  [key]
  (when (keyword? key)
    (re-find #"^obs\-" (name key))))

(defn parse
  "Parses a string and returns the simplified parse tree if it is a valid email
  and nil otherwise. Because RFC 5322 is ambiguous, the returned parse tree
  is the one with the least number of obsolete tokens."
  [message-text mode]
  (log/debugf "Parsing message using %s mode ..." mode)
  (log/tracef "Parsing message:\n"  message-text)
  (let [result (instaparse/parse (make-parser mode) message-text)]
    (if (instaparse/failure? result)
      (throw (new ParserException "Parse failure" (instaparse/get-failure result)))
      result)))

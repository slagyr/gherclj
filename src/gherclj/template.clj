(ns gherclj.template
  (:require [clojure.string :as str]))

(def ^:private type-patterns
  {"int"   {:regex "\\d+" :coerce `parse-long}
   "float" {:regex "[\\d.]+" :coerce `parse-double}})

(def ^:private regex-special-chars
  #{\. \^ \$ \* \+ \? \( \) \[ \] \{ \} \\ \|})

(defn- escape-regex-char [c]
  (if (regex-special-chars c)
    (str \\ c)
    (str c)))

(defn- parse-capture
  "Parse a {name} or {name:type} capture expression (without braces).
   Returns {:name str :coerce fn-symbol :regex str}."
  [expr]
  (let [[name type-str] (str/split expr #":" 2)
        type-info (get type-patterns type-str)]
    (if type-info
      {:name name :regex (:regex type-info) :coerce (:coerce type-info)}
      {:name name :regex "\\S+" :coerce `identity})))

(defn- parse-quoted-capture
  "Parse a \"{name}\" capture. Returns {:name str :coerce fn :regex str}."
  [expr]
  {:name expr :regex "[^\"]+" :coerce `identity})

(defn compile-template
  "Compile a step template string into {:regex Pattern :bindings [{:name str :coerce fn}]}.

   Template syntax:
     \"{name}\"  → quoted string capture
     {name:int}  → integer capture
     {name:float} → float capture
     {name}      → word capture (\\S+)"
  [template]
  (let [;; Tokenize: split into literal text, quoted captures, and bare captures
        ;; Pattern matches: \"{...}\" or {...}
        token-re #"\"?\{([^}]+)\}\"?"
        matcher (re-matcher token-re template)
        result (loop [pos 0
                      regex-parts []
                      bindings []]
                 (if (.find matcher)
                   (let [match-start (.start matcher)
                         match-end (.end matcher)
                         full-match (.group matcher)
                         inner (.group matcher 1)
                         ;; literal text before this match
                         literal (subs template pos match-start)
                         escaped-literal (apply str (map escape-regex-char literal))
                         quoted? (and (str/starts-with? full-match "\"")
                                      (str/ends-with? full-match "\""))
                         capture (if quoted?
                                   (parse-quoted-capture inner)
                                   (parse-capture inner))
                         capture-regex (if quoted?
                                         (str "\"(" (:regex capture) ")\"")
                                         (str "(" (:regex capture) ")"))]
                     (recur match-end
                            (conj regex-parts escaped-literal capture-regex)
                            (conj bindings {:name (:name capture) :coerce (:coerce capture)})))
                   ;; No more matches — append remaining literal
                   (let [remaining (subs template pos)
                         escaped-remaining (apply str (map escape-regex-char remaining))]
                     {:regex-str (str "^" (apply str (conj regex-parts escaped-remaining)) "$")
                      :bindings bindings})))]
    {:regex (re-pattern (:regex-str result))
     :bindings (mapv (fn [{:keys [name coerce]}]
                       {:name name :coerce (deref (resolve coerce))})
                     (:bindings result))}))

(defn match-step
  "Match step text against a compiled template. Returns a vector of coerced
   values on match, or nil if no match."
  [{:keys [regex bindings]} text]
  (when-let [match (re-matches regex text)]
    (let [groups (if (string? match) [] (vec (rest match)))]
      (mapv (fn [group {:keys [coerce]}]
              (coerce group))
            groups
            bindings))))

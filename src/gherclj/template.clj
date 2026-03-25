(ns gherclj.template
  (:require [clojure.string :as str]))

(def ^:private type-patterns
  {"int"    {:regex "\\d+" :coerce parse-long}
   "float"  {:regex "[\\d.]+" :coerce parse-double}
   "string" {:regex ".+" :coerce (fn [s]
                                        (if (and (str/starts-with? s "\"")
                                                 (str/ends-with? s "\""))
                                          (subs s 1 (dec (count s)))
                                          s))}})

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
      {:name name :regex "\\S+" :coerce identity})))

(defn compile-template
  "Compile a step template string into {:regex Pattern :bindings [{:name str :coerce fn}]}.

   Template syntax:
     {name:string} → greedy string capture (bounded by surrounding literal)
     {name:int}    → integer capture
     {name:float}  → float capture
     {name}        → word capture (\\S+)"
  [template]
  (let [token-re #"\{([^}]+)\}"
        matcher (re-matcher token-re template)
        result (loop [pos 0
                      regex-parts []
                      bindings []]
                 (if (.find matcher)
                   (let [match-start (.start matcher)
                         match-end (.end matcher)
                         inner (.group matcher 1)
                         literal (subs template pos match-start)
                         escaped-literal (apply str (map escape-regex-char literal))
                         capture (parse-capture inner)
                         capture-regex (str "(" (:regex capture) ")")]
                     (recur match-end
                            (conj regex-parts escaped-literal capture-regex)
                            (conj bindings {:name (:name capture) :coerce (:coerce capture)})))
                   (let [remaining (subs template pos)
                         escaped-remaining (apply str (map escape-regex-char remaining))]
                     {:regex-str (str "^" (apply str (conj regex-parts escaped-remaining)) "$")
                      :bindings bindings})))]
    {:regex (re-pattern (:regex-str result))
     :bindings (:bindings result)}))

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

;; mutation-tested: 2026-03-25
(ns gherclj.template
  (:require [clojure.string :as str]))

(defn- coerce-string
  "Strip surrounding quotes from a string value, if present."
  [s]
  (if (and (str/starts-with? s "\"")
           (str/ends-with? s "\""))
    (subs s 1 (dec (count s)))
    s))

(def ^:private type-patterns
  {"int"    {:regex "\\d+" :coerce parse-long}
   "float"  {:regex "[\\d.]+" :coerce parse-double}
   "string" {:regex ".+" :coerce coerce-string}})

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
        literals (str/split template token-re -1)
        captures (mapv second (re-seq token-re template))
        pairs (map vector literals (concat captures [nil]))
        {:keys [regex-parts bindings]}
        (reduce (fn [{:keys [regex-parts bindings]} [literal capture-expr]]
                  (let [escaped-literal (apply str (map escape-regex-char literal))]
                    (if capture-expr
                      (let [capture (parse-capture capture-expr)
                            capture-regex (str "(" (:regex capture) ")")]
                        {:regex-parts (conj regex-parts escaped-literal capture-regex)
                         :bindings (conj bindings {:name (:name capture) :coerce (:coerce capture)})})
                      {:regex-parts (conj regex-parts escaped-literal)
                       :bindings bindings})))
                {:regex-parts [] :bindings []}
                pairs)]
    {:regex (re-pattern (str "^" (apply str regex-parts) "$"))
     :bindings bindings}))

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

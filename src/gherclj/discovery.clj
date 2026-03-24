(ns gherclj.discovery
  (:require [clojure.string :as str]))

(defn- glob->regex
  "Convert a glob pattern string to a regex pattern.
   * matches any sequence of non-dot characters within a segment."
  [pattern]
  (-> pattern
      (str/replace "." "\\.")
      (str/replace "*" "[^.]*")
      (->> (format "^%s$"))
      re-pattern))

(defn- matches-glob? [pattern ns-sym]
  (re-matches (glob->regex pattern) (str ns-sym)))

(defn resolve-step-namespaces
  "Resolve step namespace entries. Symbols pass through unchanged.
   Strings are treated as glob patterns matched against available-nses."
  [entries available-nses]
  (vec (mapcat (fn [entry]
                 (if (string? entry)
                   (filter #(matches-glob? entry %) available-nses)
                   [entry]))
               entries)))

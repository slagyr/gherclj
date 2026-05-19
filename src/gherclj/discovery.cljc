;; mutation-tested: 2026-03-25
(ns gherclj.discovery
  (:require [clojure.string :as str]))

(defn- glob->regex
  "Convert a glob pattern string to a regex pattern.
   * matches any sequence of non-dot characters within a segment.
   ** crosses segment boundaries:
     .**. — zero or more inner segments (dots collapse)
     **. (at start) — zero or more leading segments
     .** (at end) — zero or more trailing segments
     ** elsewhere — one or more characters (may include dots)"
  [pattern]
  (-> pattern
      (str/replace #"\.\*\*\." "<<MID>>")
      (str/replace #"^\*\*\." "<<START>>")
      (str/replace #"\.\*\*$" "<<END>>")
      (str/replace "**" "<<BARE>>")
      (str/replace "." "\\.")
      (str/replace "*" "[^.]*")
      (str/replace "<<MID>>" "(\\.[^.]+)*\\.")
      (str/replace "<<START>>" "([^.]+\\.)*")
      (str/replace "<<END>>" "(\\.[^.]+)*")
      (str/replace "<<BARE>>" ".+")
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

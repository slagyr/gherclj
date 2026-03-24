(ns gherclj.parser
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private step-keywords #{"Given" "When" "Then" "And" "But"})

(defn- step-keyword? [trimmed]
  (some #(str/starts-with? trimmed (str % " ")) step-keywords))

(defn- strip-keyword [trimmed]
  (let [space-idx (str/index-of trimmed " ")]
    (when space-idx
      (subs trimmed (inc space-idx)))))

(defn- gherkin-type-for-keyword [trimmed]
  (cond
    (str/starts-with? trimmed "Given ") :given
    (str/starts-with? trimmed "When ")  :when
    (str/starts-with? trimmed "Then ")  :then
    (str/starts-with? trimmed "And ")   :and
    (str/starts-with? trimmed "But ")   :but
    :else                               nil))

(defn- parse-table-line
  "Parse a pipe-delimited table row into a vector of cell strings."
  [line]
  (->> (str/split line #"[|]" -1)
       rest
       butlast
       (mapv str/trim)))

(defn- table-line? [trimmed]
  (str/starts-with? trimmed "|"))

(defn- doc-string-fence? [trimmed]
  (= "\"\"\"" trimmed))

(defn- attach-table
  "Attach parsed table data to an IR node, if table-lines are present."
  [ir-node table-lines]
  (if (seq table-lines)
    (let [parsed (mapv parse-table-line table-lines)]
      (assoc ir-node :table {:headers (first parsed)
                             :rows (vec (rest parsed))}))
    ir-node))

(defn- add-step [scenario ir-node]
  (update scenario :steps (fnil conj []) ir-node))

(defn- attach-doc-string
  "Attach a doc-string to the last step in the state."
  [state]
  (if (seq (:pending-doc-string state))
    (let [steps (get-in state [:scenario :steps])
          last-step (peek steps)
          doc-str (str/join "\n" (:pending-doc-string state))
          updated-step (assoc last-step :doc-string doc-str)
          updated-steps (conj (pop steps) updated-step)]
      (-> state
          (assoc-in [:scenario :steps] updated-steps)
          (dissoc :pending-doc-string :in-doc-string)))
    state))

(defn- process-step-entry [state entry]
  (cond
    ;; Inside a doc-string: accumulate or close
    (:in-doc-string state)
    (if (doc-string-fence? entry)
      (attach-doc-string state)
      (update state :pending-doc-string (fnil conj []) entry))

    ;; Opening doc-string fence
    (doc-string-fence? entry)
    (assoc state :in-doc-string true :pending-doc-string [])

    ;; Table line
    (table-line? entry)
    (update state :pending-table (fnil conj []) entry)

    ;; Step keyword — flush pending table first
    :else
    (let [state (if (seq (:pending-table state))
                  (let [steps (get-in state [:scenario :steps])
                        last-step (peek steps)
                        updated-step (attach-table last-step (:pending-table state))
                        updated-steps (conj (pop steps) updated-step)]
                    (-> state
                        (assoc-in [:scenario :steps] updated-steps)
                        (dissoc :pending-table)))
                  state)
          gherkin-type (gherkin-type-for-keyword entry)]
      (when gherkin-type
        (let [text (strip-keyword entry)
              ir-node {:type gherkin-type :text text}]
          (update state :scenario add-step ir-node))))))

(defn- finalize-pending-attachments
  "Attach any remaining pending table or doc-string to the last step."
  [state]
  (cond-> state
    (seq (:pending-table state))
    (as-> s (let [steps (get-in s [:scenario :steps])
                  last-step (peek steps)
                  updated-step (attach-table last-step (:pending-table s))
                  updated-steps (conj (pop steps) updated-step)]
              (-> s
                  (assoc-in [:scenario :steps] updated-steps)
                  (dissoc :pending-table))))
    (seq (:pending-doc-string state))
    attach-doc-string))

(defn- parse-scenario-lines [lines]
  (let [result (-> (reduce process-step-entry
                           {:scenario {:steps []}}
                           lines)
                   finalize-pending-attachments)]
    (:scenario result)))

(defn- tag-line? [trimmed]
  (str/starts-with? trimmed "@"))

(defn- has-wip-tag? [trimmed]
  (some #(= "@wip" %) (str/split trimmed #"\s+")))

(defn- append-to-current-scenario [result trimmed]
  (let [scenarios (:scenarios result)
        current (peek scenarios)
        updated (update current :lines conj trimmed)]
    (assoc result :scenarios (conj (pop scenarios) updated))))

(defn- parse-sections
  "Splits feature lines into sections: feature line, description, background, and scenarios."
  [lines]
  (loop [lines lines
         state :start
         wip-pending false
         in-doc-string false
         result {:feature-line nil
                 :description-lines []
                 :background-lines []
                 :scenarios []}]
    (if (empty? lines)
      result
      (let [line (first lines)
            trimmed (str/trim line)
            rest-lines (rest lines)]
        (cond
          ;; Inside a doc-string: pass everything through until closing fence
          (and in-doc-string (doc-string-fence? trimmed))
          (let [result (if (= state :background)
                         (update result :background-lines conj trimmed)
                         (append-to-current-scenario result trimmed))]
            (recur rest-lines state wip-pending false result))

          in-doc-string
          (let [result (if (= state :background)
                         (update result :background-lines conj trimmed)
                         (append-to-current-scenario result trimmed))]
            (recur rest-lines state wip-pending true result))

          ;; Opening doc-string fence
          (doc-string-fence? trimmed)
          (let [result (if (= state :background)
                         (update result :background-lines conj trimmed)
                         (append-to-current-scenario result trimmed))]
            (recur rest-lines state wip-pending true result))

          (and (= state :start) (str/starts-with? trimmed "Feature:"))
          (recur rest-lines :description false false
                 (assoc result :feature-line trimmed))

          (and (= state :description) (str/blank? trimmed))
          (recur rest-lines :description false false result)

          (str/starts-with? trimmed "Background:")
          (recur rest-lines :background false false result)

          (tag-line? trimmed)
          (recur rest-lines state (has-wip-tag? trimmed) false result)

          (str/starts-with? trimmed "Scenario:")
          (let [title (str/trim (subs trimmed 9))
                scenario-entry {:title title :lines [] :wip wip-pending}]
            (recur rest-lines :scenario false false
                   (update result :scenarios conj scenario-entry)))

          (and (= state :background) (step-keyword? trimmed))
          (recur rest-lines :background false false
                 (update result :background-lines conj trimmed))

          (and (= state :background) (table-line? trimmed))
          (recur rest-lines :background false false
                 (update result :background-lines conj trimmed))

          (and (= state :scenario) (step-keyword? trimmed))
          (recur rest-lines :scenario false false
                 (append-to-current-scenario result trimmed))

          (and (= state :scenario) (table-line? trimmed))
          (recur rest-lines :scenario false false
                 (append-to-current-scenario result trimmed))

          (and (= state :description) (seq trimmed))
          (recur rest-lines :description false false
                 (update result :description-lines conj trimmed))

          :else
          (recur rest-lines state wip-pending false result))))))

(defn parse-feature
  "Parse a Gherkin feature string into an EDN IR map."
  [text]
  (let [lines (str/split-lines text)
        sections (parse-sections lines)
        feature-name (str/trim (subs (:feature-line sections) 8))
        description-lines (:description-lines sections)
        background-lines (:background-lines sections)
        scenarios (mapv (fn [{:keys [title lines wip]}]
                          (let [parsed (parse-scenario-lines lines)]
                            (cond-> (assoc parsed :scenario title)
                              wip (assoc :wip true))))
                        (:scenarios sections))]
    (cond-> {:feature feature-name
             :scenarios scenarios}
      (seq description-lines)
      (assoc :description (str/join "\n" description-lines))

      (seq background-lines)
      (assoc :background (parse-scenario-lines background-lines)))))

(defn parse-feature-file
  "Parse a .feature file into an EDN IR map with :source."
  [path]
  (let [content (slurp path)
        filename (last (str/split path #"/"))]
    (assoc (parse-feature content) :source filename)))

(defn parse-features-dir
  "Parse all .feature files in a directory. Returns a seq of IR maps."
  [dir-path]
  (let [dir (io/file dir-path)]
    (->> (.listFiles dir)
         (filter #(str/ends-with? (.getName %) ".feature"))
         (sort-by #(.getName %))
         (mapv #(parse-feature-file (.getPath %))))))

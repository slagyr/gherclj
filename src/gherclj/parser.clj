;; mutation-tested: 2026-03-25
(ns gherclj.parser
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private step-keywords #{"Given" "When" "Then" "And" "But"})

(defn- normalize-path [path]
  (-> path
      (str/replace "\\" "/")
      (str/replace #"^\./" "")))

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

(defn- entry-text
  "Scenario/background lines may be plain strings or {:text :line} maps."
  [entry]
  (if (map? entry) (:text entry) entry))

(defn- entry-line
  "1-based feature line number when the entry carries provenance."
  [entry]
  (when (map? entry) (:line entry)))

(defn- attach-table
  "Attach parsed table data to an IR node, if table-lines are present.
   Table lines may be plain strings or {:text :line} maps. When lines are
   present, :header-line and :row-lines are recorded for failure provenance."
  [ir-node table-lines]
  (if (seq table-lines)
    (let [texts  (mapv entry-text table-lines)
          lines  (mapv entry-line table-lines)
          parsed (mapv parse-table-line texts)
          table  (cond-> {:headers (first parsed)
                          :rows (vec (rest parsed))}
                   (first lines) (assoc :header-line (first lines))
                   (some identity (rest lines))
                   (assoc :row-lines (vec (rest lines))))]
      (assoc ir-node :table table))
    ir-node))

(defn- add-step [scenario ir-node]
  (update scenario :steps (fnil conj []) ir-node))

(defn- attach-doc-string
  "Attach a doc-string to the last step in the state."
  [state]
  (if (seq (:pending-doc-string state))
    (let [steps (get-in state [:scenario :steps])
          last-step (peek steps)
          doc-str (-> (str/join "\n" (:pending-doc-string state))
                      (str/replace "\\\"\\\"\\\"" "\"\"\""))
          updated-step (assoc last-step :doc-string doc-str)
          updated-steps (conj (pop steps) updated-step)]
      (-> state
          (assoc-in [:scenario :steps] updated-steps)
          (dissoc :pending-doc-string :in-doc-string)))
    state))

(defn- process-step-entry [state entry]
  (let [text (entry-text entry)
        line (entry-line entry)]
    (cond
      (:in-doc-string state)
      (if (doc-string-fence? text)
        (attach-doc-string state)
        (update state :pending-doc-string (fnil conj []) text))

      (doc-string-fence? text)
      (assoc state :in-doc-string true :pending-doc-string [])

      (table-line? text)
      (update state :pending-table (fnil conj []) entry)

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
            gherkin-type (gherkin-type-for-keyword text)]
        (when gherkin-type
          (let [step-text (strip-keyword text)
                ir-node (cond-> {:type gherkin-type :text step-text}
                          line (assoc :line line))]
            (update state :scenario add-step ir-node)))))))
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

(defn- parse-tags [trimmed]
  (->> (str/split trimmed #"\s+")
       (filter #(str/starts-with? % "@"))
       (mapv #(subs % 1))))

;; --- parse-sections helpers ---

(defn- numbered-entry
  "Capture trimmed text with optional 1-based line from the section context."
  [ctx text]
  (cond-> {:text text}
    (:line ctx) (assoc :line (:line ctx))))

(defn- append-to-current-scenario [result entry]
  (let [scenarios (:scenarios result)
        current (peek scenarios)
        updated (update current :lines conj entry)]
    (assoc result :scenarios (conj (pop scenarios) updated))))

(defn- append-examples-line [result entry]
  (let [scenarios (:scenarios result)
        current (peek scenarios)
        updated (update current :examples-lines (fnil conj []) entry)]
    (assoc result :scenarios (conj (pop scenarios) updated))))

(defn- append-line-to-section [{:keys [section] :as ctx} content]
  (let [entry (if (map? content) content (numbered-entry ctx content))]
    (if (= section :background)
      (update ctx :result update :background-lines conj entry)
      (update ctx :result append-to-current-scenario entry))))
(defn- doc-string-indent [line]
  (- (count line) (count (str/triml line))))

(defn- doc-string-content [line indent]
  (if (> (count line) indent)
    (subs line indent)
    (str/trim line)))

;; --- Line processors ---
;; Each returns an updated ctx map, or nil to fall through.

(defn- process-doc-string-close [ctx trimmed]
  (when (and (:in-doc-string ctx) (doc-string-fence? trimmed))
    (-> ctx
        (append-line-to-section trimmed)
        (assoc :in-doc-string false :doc-string-indent 0))))

(defn- process-doc-string-content [ctx line]
  (when (:in-doc-string ctx)
    (let [content (doc-string-content line (:doc-string-indent ctx))]
      (append-line-to-section ctx content))))

(defn- process-doc-string-open [ctx trimmed line]
  (when (doc-string-fence? trimmed)
    (-> ctx
        (append-line-to-section trimmed)
        (assoc :in-doc-string true
               :doc-string-indent (doc-string-indent line)))))

(defn- process-feature-line [ctx trimmed]
  (when (and (= :start (:section ctx))
             (str/starts-with? trimmed "Feature:"))
    (-> ctx
        (assoc :section :description :tags-pending [])
        (update :result assoc
                :feature-line trimmed
                :feature-tags (:tags-pending ctx)))))

(defn- process-blank-in-description [ctx trimmed]
  (when (and (= :description (:section ctx))
             (str/blank? trimmed))
    (assoc ctx :tags-pending [])))

(defn- process-description-text [ctx trimmed]
  (when (and (= :description (:section ctx))
             (seq trimmed))
    (-> ctx
        (assoc :tags-pending [])
        (update :result update :description-lines conj trimmed))))

(defn- process-background [ctx trimmed]
  (when (str/starts-with? trimmed "Background:")
    (assoc ctx :section :background :tags-pending [])))

(defn- process-tag-line [ctx trimmed]
  (when (tag-line? trimmed)
    (update ctx :tags-pending into (parse-tags trimmed))))

(defn- process-scenario-outline [ctx trimmed]
  (when (str/starts-with? trimmed "Scenario Outline:")
    (let [title (str/trim (subs trimmed 17))
          entry (cond-> {:title title :lines [] :tags (:tags-pending ctx)
                         :outline? true :examples-lines []}
                  (:line ctx) (assoc :line (:line ctx)))]
      (-> ctx
          (assoc :section :scenario :tags-pending [])
          (update :result update :scenarios conj entry)))))

(defn- process-scenario [ctx trimmed]
  (when (str/starts-with? trimmed "Scenario:")
    (let [title (str/trim (subs trimmed 9))
          entry (cond-> {:title title :lines [] :tags (:tags-pending ctx)}
                  (:line ctx) (assoc :line (:line ctx)))]
      (-> ctx
          (assoc :section :scenario :tags-pending [])
          (update :result update :scenarios conj entry)))))

(defn- process-examples [ctx trimmed]
  (when (and (= :scenario (:section ctx))
             (str/starts-with? trimmed "Examples:"))
    (assoc ctx :section :examples :tags-pending [])))

(defn- process-background-line [ctx trimmed]
  (when (and (= :background (:section ctx))
             (or (step-keyword? trimmed) (table-line? trimmed)))
    (-> ctx
        (assoc :tags-pending [])
        (update :result update :background-lines conj (numbered-entry ctx trimmed)))))

(defn- process-scenario-step [ctx trimmed]
  (when (and (#{:scenario :examples} (:section ctx))
             (step-keyword? trimmed))
    (-> ctx
        (assoc :section :scenario :tags-pending [])
        (update :result append-to-current-scenario (numbered-entry ctx trimmed)))))

(defn- process-examples-table [ctx trimmed]
  (when (and (= :examples (:section ctx))
             (table-line? trimmed))
    (-> ctx
        (assoc :tags-pending [])
        (update :result append-examples-line (numbered-entry ctx trimmed)))))

(defn- process-scenario-table [ctx trimmed]
  (when (and (= :scenario (:section ctx))
             (table-line? trimmed))
    (-> ctx
        (assoc :tags-pending [])
        (update :result append-to-current-scenario (numbered-entry ctx trimmed)))))

(defn- process-line
  "Classify and process a single line. Returns updated ctx."
  [ctx line]
  (let [trimmed (str/trim line)]
    (or (process-doc-string-close ctx trimmed)
        (process-doc-string-content ctx line)
        (process-doc-string-open ctx trimmed line)
        (process-feature-line ctx trimmed)
        (process-blank-in-description ctx trimmed)
        (process-background ctx trimmed)
        (process-tag-line ctx trimmed)
        (process-scenario-outline ctx trimmed)
        (process-scenario ctx trimmed)
        (process-examples ctx trimmed)
        (process-background-line ctx trimmed)
        (process-scenario-step ctx trimmed)
        (process-examples-table ctx trimmed)
        (process-scenario-table ctx trimmed)
        (process-description-text ctx trimmed)
        ctx)))

(defn- parse-sections
  "Splits feature lines into sections: feature line, description, background, and scenarios.
   Each physical line is tagged with a 1-based :line for provenance."
  [lines]
  (let [initial {:section :start
                 :tags-pending []
                 :in-doc-string false
                 :doc-string-indent 0
                 :result {:feature-line nil
                          :feature-tags []
                          :description-lines []
                          :background-lines []
                          :scenarios []}}]
    (:result (reduce (fn [ctx [idx line]]
                       (process-line (assoc ctx :line (inc idx)) line))
                     initial
                     (map-indexed vector lines)))))
;; --- Outline expansion ---

(defn- substitute-placeholders
  "Replace <placeholder> in text with values from the row map."
  [text row-map]
  (reduce-kv (fn [s k v] (str/replace s (str "<" k ">") v))
             text row-map))

(defn- expand-outline
  "Expand a Scenario Outline into concrete scenarios.
   Step :line values come from the outline body; scenario :line from the outline header."
  [{:keys [title lines tags examples-lines line]}]
  (let [parsed-steps (:steps (parse-scenario-lines lines))
        examples-table (mapv (comp parse-table-line entry-text) examples-lines)
        headers (first examples-table)
        rows (rest examples-table)]
    (mapv (fn [row]
            (let [row-map (zipmap headers row)
                  scenario-name (str title " — " (str/join ", " row))
                  expanded-steps (mapv (fn [step]
                                         (update step :text #(substitute-placeholders % row-map)))
                                       parsed-steps)]
              (cond-> {:scenario scenario-name :steps expanded-steps}
                line (assoc :line line)
                (seq tags) (assoc :tags tags))))
          rows)))
;; --- Public API ---

(defn parse-feature
  "Parse a Gherkin feature string into an EDN IR map."
  [text]
  (when (str/blank? text)
    (throw (RuntimeException. "Cannot parse empty feature file")))
  (let [lines (str/split-lines text)
        sections (parse-sections lines)]
    (when-not (:feature-line sections)
      (throw (RuntimeException. "Missing Feature keyword — expected a line starting with 'Feature:'")))
    (let [feature-name (str/trim (subs (:feature-line sections) 8))
        description-lines (:description-lines sections)
        background-lines (:background-lines sections)
        feature-tags (:feature-tags sections)
        scenarios (->> (:scenarios sections)
                       (mapcat (fn [{:keys [outline?] :as entry}]
                                 (if outline?
                                   (expand-outline (update entry :tags #(into feature-tags %)))
                                   (let [{:keys [title lines tags line]} entry
                                         parsed (parse-scenario-lines lines)
                                         all-tags (into feature-tags tags)]
                                     [(cond-> (assoc parsed :scenario title)
                                        line (assoc :line line)
                                        (seq all-tags) (assoc :tags all-tags))]))))
                       vec)]
    (cond-> {:feature feature-name
             :scenarios scenarios}
      (seq feature-tags)
      (assoc :tags feature-tags)

      (seq description-lines)
      (assoc :description (str/join "\n" description-lines))

      (seq background-lines)
      (assoc :background (parse-scenario-lines background-lines))))))

(defn parse-feature-file
  "Parse a .feature file into an EDN IR map with :source (relative path)."
  ([root-dir file]
   (parse-feature-file root-dir nil file))
  ([root-dir source-prefix file]
   (let [content (slurp file)
         rel-path (str (.relativize (.toPath (io/file root-dir)) (.toPath file)))
         source (if source-prefix
                  (str (normalize-path source-prefix) "/" rel-path)
                  rel-path)]
     (assoc (parse-feature content) :source source))))

(defn- root-spec [dir-spec]
  (if (map? dir-spec)
    dir-spec
    {:root dir-spec
     :path dir-spec}))

(defn parse-features-dir
  "Parse all .feature files in a directory, recursively. Returns a seq of IR maps."
  [dir-specs]
  (let [roots (if (sequential? dir-specs) (mapv root-spec dir-specs) [(root-spec dir-specs)])
        multi-root? (> (count roots) 1)]
    (->> roots
         (mapcat (fn [{:keys [root path]}]
                   (let [dir (io/file path)]
                     (when (.exists dir)
                       (->> (file-seq dir)
                            (filter #(str/ends-with? (.getName %) ".feature"))
                            (sort-by #(str (.toPath %)))
                            (mapv #(parse-feature-file path (when multi-root? root) %)))))))
         (sort-by :source)
         vec)))

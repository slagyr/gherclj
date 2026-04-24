(ns gherclj.features.steps.tags
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.generator :as gen]
            [clojure.string :as str]))

(defgiven tagged-scenarios "a feature with tagged scenarios:"
  [table]
  (let [{:keys [headers rows]} table
        scenarios (mapv (fn [row]
                          (let [m (zipmap headers row)
                                name (clojure.core/get m "scenario")
                                tags-str (clojure.core/get m "tags")
                                tags (when (seq tags-str)
                                       (str/split tags-str #","))]
                            (cond-> {:scenario name
                                     :steps [{:type :given :text "something"}]}
                              (seq tags) (assoc :tags tags))))
                        rows)]
    (g/assoc! :feature-ir {:feature "Test" :source "test.feature" :scenarios scenarios})))

(defwhen generate-with-include-tags "generating with include tags {tags:string}"
  [tags]
  (let [tag-list (str/split tags #",\s*")
        config {:step-namespaces []
                :include-tags tag-list
                :exclude-tags []
                :framework :speclj}]
    (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir)))))

(defwhen generate-with-exclude-tags "generating with exclude tags {tags:string}"
  [tags]
  (let [tag-list (str/split tags #",\s*")
        config {:step-namespaces []
                :exclude-tags tag-list
                :framework :speclj}]
    (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir)))))

(defwhen generate-with-no-filters "generating with no tag filters"
  []
  (let [config {:step-namespaces []
                :framework :speclj}]
    (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir)))))

(defthen generated-scenarios-should-be "the generated scenarios should be {list:string}"
  [list]
  (let [expected (mapv str/trim (str/split list #","))
        output (g/get :generated-output)
        found (mapv second (re-seq #"\(it \"([^\"]+)\"" output))]
    (g/should= expected found)))

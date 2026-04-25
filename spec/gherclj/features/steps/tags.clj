(ns gherclj.features.steps.tags
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.generator :as gen]
            [clojure.string :as str]))

(helper! gherclj.features.steps.tags)

(defn tagged-scenarios! [table]
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

(defn generate-with-include-tags! [tags]
  (let [tag-list (str/split tags #",\s*")
        config {:step-namespaces []
                :include-tags tag-list
                :exclude-tags []
                :framework :speclj}]
    (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir)))))

(defn generate-with-exclude-tags! [tags]
  (let [tag-list (str/split tags #",\s*")
        config {:step-namespaces []
                :exclude-tags tag-list
                :framework :speclj}]
    (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir)))))

(defn generate-with-no-filters! []
  (let [config {:step-namespaces []
                :framework :speclj}]
    (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir)))))

(defn generated-scenarios-should-be [list]
  (let [expected (mapv str/trim (str/split list #","))
        output (g/get :generated-output)
        found (mapv second (re-seq #"\(it \"([^\"]+)\"" output))]
    (g/should= expected found)))

(defgiven "a feature with tagged scenarios:" tags/tagged-scenarios!)

(defwhen "generating with include tags {tags:string}" tags/generate-with-include-tags!)

(defwhen "generating with exclude tags {tags:string}" tags/generate-with-exclude-tags!)

(defwhen "generating with no tag filters" tags/generate-with-no-filters!)

(defthen "the generated scenarios should be {list:string}" tags/generated-scenarios-should-be)

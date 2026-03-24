(ns gherclj.config
  (:require [c3kit.apron.schema :as schema]))

(def pipeline-schema
  {:features-dir    {:type :string}
   :edn-dir         {:type    :string
                     :coerce  (fn [v] (or v "target/gherclj/edn"))}
   :output-dir      {:type    :string
                     :coerce  (fn [v] (or v "target/gherclj/generated"))}
   :step-namespaces {:type    :seq
                     :coerce  (fn [v] (or v []))}
   :test-framework  {:type     :keyword
                     :validate #(contains? #{:speclj :clojure.test} %)}
   :verbose         {:type   :boolean
                     :coerce (fn [v] (boolean v))}})

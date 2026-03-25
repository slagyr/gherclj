(ns gherclj.features.steps.template-compilation
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.template :as template]
            [clojure.edn :as edn]
            [speclj.core :refer [should= should-not-be-nil]]))

(defgiven set-template "a template {template:string}"
  [template]
  (g/assoc! :template-str template))

(defwhen compile-template "the template is compiled"
  []
  (g/assoc! :compiled (template/compile-template (g/get :template-str))))

(defthen regex-should-be "the regex should be {expected:string}"
  [expected]
  (should= expected (str (:regex (g/get :compiled)))))

(defthen regex-should-match "the regex should match {text:string}"
  [text]
  (let [result (template/match-step (g/get :compiled) text)]
    (g/assoc! :match-result result)
    (should-not-be-nil result)))

(defthen binding-count "there should be {count:int} bindings"
  [count]
  (should= count (clojure.core/count (:bindings (g/get :compiled)))))

(defthen captured-value-should-be "the captured value should be {expected:string}"
  [expected]
  (let [match-result (g/get :match-result)
        actual (first match-result)]
    (if (number? actual)
      (should= (parse-long expected) actual)
      (should= expected actual))))

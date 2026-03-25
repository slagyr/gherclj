(ns gherclj.features.steps.template-compilation
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.template :as template]))

(defgiven set-template "a template {template:string}"
  [template]
  (g/assoc! :template-str template))

(defwhen compile-template "the template is compiled"
  []
  (g/assoc! :compiled (template/compile-template (g/get :template-str))))

(defthen regex-should-be "the regex should be {expected:string}"
  [expected]
  (g/should= expected (str (:regex (g/get :compiled)))))

(defthen regex-should-match "the regex should match {text:string}"
  [text]
  (let [result (template/match-step (g/get :compiled) text)]
    (g/assoc! :match-result result)
    (g/should-not-be-nil result)))

(defthen binding-count "there should be {count:int} bindings"
  [count]
  (g/should= count (clojure.core/count (:bindings (g/get :compiled)))))

(defthen captured-value-should-be "the captured value should be {expected:string}"
  [expected]
  (let [match-result (g/get :match-result)
        actual (first match-result)]
    (if (number? actual)
      (g/should= (parse-long expected) actual)
      (g/should= expected actual))))

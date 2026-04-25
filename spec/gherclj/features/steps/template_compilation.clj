(ns gherclj.features.steps.template-compilation
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.template :as template]))

(helper! gherclj.features.steps.template-compilation)

(defn set-template! [template]
  (g/assoc! :template-str template))

(defn compile-template! []
  (g/assoc! :compiled (template/compile-template (g/get :template-str))))

(defn regex-should-be [expected]
  (g/should= expected (str (:regex (g/get :compiled)))))

(defn regex-should-match [text]
  (let [result (template/match-step (g/get :compiled) text)]
    (g/assoc! :match-result result)
    (g/should-not-be-nil result)))

(defn binding-count [count]
  (g/should= count (clojure.core/count (:bindings (g/get :compiled)))))

(defn captured-value-should-be [expected]
  (let [match-result (g/get :match-result)
        actual (first match-result)]
    (if (number? actual)
      (g/should= (parse-long expected) actual)
      (g/should= expected actual))))

(defgiven "a template {template:string}" template-compilation/set-template!)

(defwhen "the template is compiled" template-compilation/compile-template!)

(defthen "the regex should be {expected:string}" template-compilation/regex-should-be)

(defthen "the regex should match {text:string}" template-compilation/regex-should-match)

(defthen "there should be {count:int} bindings" template-compilation/binding-count)

(defthen "the captured value should be {expected:string}" template-compilation/captured-value-should-be)

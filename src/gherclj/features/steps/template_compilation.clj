(ns gherclj.features.steps.template-compilation
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.template :as template]
            [speclj.core :refer [should= should-not-be-nil]]))

(defgiven set-template "a template \"{template}\""
  [template]
  (g/assoc! :template-str template))

(defwhen compile-template "the template is compiled"
  []
  (g/assoc! :compiled (template/compile-template (g/get :template-str))))

(defthen regex-should-be "the regex should be \"{expected}\""
  [expected]
  (should= expected (str (:regex (g/get :compiled)))))

(defthen regex-should-match "the regex should match \"{text}\""
  [text]
  (let [result (template/match-step (g/get :compiled) text)]
    (g/assoc! :match-result result)
    (should-not-be-nil result)))

(defthen binding-count "there should be {count:int} bindings"
  [count]
  (should= count (clojure.core/count (:bindings (g/get :compiled)))))

(defthen captured-value-string "the captured value should be \"{expected}\""
  [expected]
  (should= [expected] (g/get :match-result)))

(defthen captured-value-int "the captured value should be {expected:int}"
  [expected]
  (should= [expected] (g/get :match-result)))

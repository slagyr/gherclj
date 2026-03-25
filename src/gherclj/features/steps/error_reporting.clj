(ns gherclj.features.steps.error-reporting
  (:require [gherclj.core :as g :refer [defthen]]
            [clojure.string :as str]))

(defthen classification-should-fail "classification should fail with message {text:string}"
  [text]
  (let [error (g/get :error)]
    (g/should-not-be-nil error)
    (g/should (str/includes? error text))))

(defthen error-should-mention "the error should mention {text:string}"
  [text]
  (let [error (g/get :error)]
    (g/should-not-be-nil error)
    (g/should (str/includes? error text))))

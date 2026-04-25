(ns gherclj.features.steps.error-reporting
  (:require [gherclj.core :as g :refer [defthen helper!]]
            [clojure.string :as str]))

(helper! gherclj.features.steps.error-reporting)

(defn classification-should-fail [text]
  (let [error (g/get :error)]
    (g/should-not-be-nil error)
    (g/should (str/includes? error text))))

(defn error-should-mention [text]
  (let [error (g/get :error)]
    (g/should-not-be-nil error)
    (g/should (str/includes? error text))))

(defthen "classification should fail with message {text:string}" error-reporting/classification-should-fail)

(defthen "the error should mention {text:string}" error-reporting/error-should-mention)

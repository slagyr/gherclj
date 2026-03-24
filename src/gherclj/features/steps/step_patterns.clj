(ns gherclj.features.steps.step-patterns
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.features.harness :as h]))

;; --- Regex-based test subjects ---

(defgiven raw-output-match #"^the output contains (.+)$"
  [text]
  text)

(defgiven raw-digit-match #"^count is (\d+)$"
  [digits]
  digits)

;; --- Steps ---

(defgiven lookup-registered-step "the registered step \"{name}\""
  [name]
  (h/register-real-step! 'gherclj.features.steps.step-patterns name))

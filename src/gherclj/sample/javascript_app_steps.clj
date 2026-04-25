(ns gherclj.sample.javascript-app-steps
  "Sample step namespace targeting :javascript/node-test generation. Used as a fixture for
   the javascript_generation feature. Distinct step phrases (JavaScript user, etc.)
   so this namespace can be loaded alongside gherclj.sample.app-steps without
   ambiguity."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.javascript.node-test :as js]))

(helper! "lib/javascript_app_steps.js")

(js/scenario-setup! "const subject = new javascript_app_steps.SampleAppSteps()")

(defgiven "a JavaScript user {name:string}" subject.create-adventurer)
(defwhen  "the JavaScript user logs in" subject.enter-the-realm)
(defthen  "the JavaScript response should be {status:int}" subject.verify-outcome)

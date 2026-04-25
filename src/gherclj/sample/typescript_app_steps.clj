(ns gherclj.sample.typescript-app-steps
  "Sample step namespace targeting :typescript/node-test generation. Used as a fixture for
   the typescript_generation feature. Distinct step phrases (TypeScript user, etc.)
   so this namespace can be loaded alongside gherclj.sample.app-steps without
   ambiguity."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.typescript.node-test :as ts]))

(helper! "lib/typescript_app_steps")

(ts/describe-setup! "let subject: typescript_app_steps.SampleAppSteps")
(ts/describe-setup! "beforeEach(() => {\n  subject = new typescript_app_steps.SampleAppSteps()\n})")

(defgiven "a TypeScript user {name:string}" subject.create-adventurer)
(defwhen  "the TypeScript user logs in" subject.enter-the-realm)
(defthen  "the TypeScript response should be {status:int}" subject.verify-outcome)

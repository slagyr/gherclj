(ns gherclj.sample.pytest-app-steps
  "Sample step namespace targeting :python/pytest generation. Used as a fixture for
   the pytest_generation feature. Distinct step phrases (Python user, etc.)
   so this namespace can be loaded alongside gherclj.sample.app-steps without
   ambiguity."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.python.pytest :as pytest]))

(helper! "sample_app")

(pytest/scenario-setup! "sut = SampleApp()")

(defgiven "a Python user {name:string}"             sut.create-adventurer)
(defwhen  "the Python user logs in"                 sut.enter-the-realm)
(defthen  "the Python response should be {status:int}" sut.verify-outcome)

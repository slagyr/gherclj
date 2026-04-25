(ns gherclj.sample.rspec-app-steps
  "Sample step namespace targeting :rspec generation. Used as a fixture for
   the rspec_generation feature. Distinct step phrases (Ruby user, etc.)
   so this namespace can be loaded alongside gherclj.sample.app-steps without
   ambiguity."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.rspec :as rspec]))

(helper! "lib/sample_app")

(rspec/describe-setup! "subject { SampleApp.new }")

(defgiven "a Ruby user {name:string}"             subject.create-adventurer)
(defwhen  "the Ruby user logs in"                  subject.enter-the-realm)
(defthen  "the Ruby response should be {status:int}" subject.verify-outcome)

(ns gherclj.sample.bash-app-steps
  "Sample step namespace targeting :bash/testing generation. Used as a fixture for
   the bash_generation feature. Distinct step phrases (Bash user, etc.)
   so this namespace can be loaded alongside gherclj.sample.app-steps without
   ambiguity."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.bash.testing :as bash]))

(helper! "lib/sample_app.sh")

(bash/scenario-setup! "subject_new")

(defgiven "a Bash user {name:string}" "subject.create-adventurer")
(defwhen  "the Bash user logs in" "subject.enter-the-realm")
(defthen  "the Bash response should be {status:int}" "subject.verify-outcome")

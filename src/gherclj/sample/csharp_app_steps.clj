(ns gherclj.sample.csharp-app-steps
  "Sample step namespace targeting :csharp/xunit generation. Used as a fixture for
   the xunit_generation feature. Distinct step phrases (CSharp user, etc.) so this
   namespace can be loaded alongside other sample step namespaces without ambiguity."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.csharp.xunit :as xunit]))

(helper! "SampleApp")
(xunit/project-reference! "lib/SampleApp.csproj")
(xunit/scenario-setup! "var subject = new SampleAppSteps();")

(defgiven "a CSharp user {name:string}" subject.createAdventurer)
(defwhen  "the CSharp user logs in" subject.enterTheRealm)
(defthen  "the CSharp response should be {status:int}" subject.verifyOutcome)

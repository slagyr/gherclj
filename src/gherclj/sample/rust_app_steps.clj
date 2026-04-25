(ns gherclj.sample.rust-app-steps
  "Sample step namespace targeting :rust/rustc-test generation. Used as a fixture for
   the rust_generation feature. Distinct step phrases (Rust user, etc.) so this
   namespace can be loaded alongside other sample step namespaces without ambiguity."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.rust.rustc-test :as rust]))

(helper! "lib/sample_app.rs")

(rust/scenario-setup! "let mut subject = sample_app::SampleAppSteps::new();")

(defgiven "a Rust user {name:string}" subject.create-adventurer)
(defwhen  "the Rust user logs in" subject.enter-the-realm)
(defthen  "the Rust response should be {status:int}" subject.verify-outcome)

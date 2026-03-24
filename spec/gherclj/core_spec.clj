(ns gherclj.core-spec
  (:require [speclj.core :refer :all]
            [gherclj.core :refer :all]
            [gherclj.core :as core]))

;; Sample step definitions — must be defined before the describe block

(defgiven sample-given-step "a project \"{slug}\" with timeout {timeout:int}"
  [slug timeout]
  :setup-result)

(defwhen sample-when-step "checking for zombies"
  []
  :action-result)

(defthen sample-then-step "session \"{session-id}\" should be a zombie with reason \"{reason}\""
  [session-id reason]
  :assert-result)

(defthen sample-regex-step #"^the output should contain headers (.+)$"
  [headers-str]
  :regex-result)

(describe "Core"

  (context "defgiven"

    (it "defines a callable function"
      (should (fn? sample-given-step))
      (should= :setup-result (sample-given-step "alpha" 300)))

    (it "registers a :given step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= "sample-given-step" (:name %)) steps))]
        (should-not-be-nil step)
        (should= :given (:type step))
        (should= "a project \"{slug}\" with timeout {timeout:int}" (:template step)))))

  (context "defwhen"

    (it "defines a callable function"
      (should (fn? sample-when-step))
      (should= :action-result (sample-when-step)))

    (it "registers a :when step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= "sample-when-step" (:name %)) steps))]
        (should-not-be-nil step)
        (should= :when (:type step)))))

  (context "defthen"

    (it "defines a callable function"
      (should (fn? sample-then-step))
      (should= :assert-result (sample-then-step "sess-1" "timeout")))

    (it "registers a :then step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= "sample-then-step" (:name %)) steps))]
        (should-not-be-nil step)
        (should= :then (:type step)))))

  (context "raw regex escape hatch"

    (it "accepts a regex pattern instead of a template string"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= "sample-regex-step" (:name %)) steps))]
        (should-not-be-nil step)
        (should-be-nil (:template step))
        (should-not-be-nil (:regex step)))))

  (context "classify-step"

    (it "matches step text to a registered step and extracts args"
      (let [steps (core/collect-steps ['gherclj.core-spec])
            result (core/classify-step steps "a project \"alpha\" with timeout 300")]
        (should-not-be-nil result)
        (should= 'gherclj.core-spec (:ns result))
        (should= "sample-given-step" (:name result))
        (should= ["alpha" 300] (:args result))))

    (it "returns nil for unrecognized step text"
      (let [steps (core/collect-steps ['gherclj.core-spec])
            result (core/classify-step steps "something completely unrecognized")]
        (should-be-nil result)))))

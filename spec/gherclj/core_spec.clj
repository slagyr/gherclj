(ns gherclj.core-spec
  (:require [speclj.core :refer :all]
            [gherclj.core :as core :refer [defgiven defwhen defthen]]))

;; Sample step definitions — must be defined before the describe block

(defgiven sample-given-step "a project {slug:string} with timeout {timeout:int}"
  [slug timeout]
  :setup-result)

(defwhen sample-when-step "checking for zombies"
  []
  :action-result)

(defthen sample-then-step "session {session-id:string} should be a zombie with reason {reason:string}"
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
        (should= "a project {slug:string} with timeout {timeout:int}" (:template step)))))

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
        (should-be-nil result)))

    (it "throws on ambiguous matches"
      (let [steps [{:name "greet-any" :regex #"^hello (\S+)$"
                    :bindings [{:name "name" :coerce identity}]
                    :ns 'test :type :given}
                   {:name "greet-world" :regex #"^hello world$"
                    :ns 'test :type :given}]]
        (should-throw RuntimeException
          (core/classify-step steps "hello world")))))

  (context "state management"

    (before (core/reset!))

    (it "starts empty after reset"
      (should= {} (core/get)))

    (it "assoc! sets a key-value"
      (core/assoc! :name "alice")
      (should= "alice" (core/get :name)))

    (it "assoc! sets multiple key-values"
      (core/assoc! :a 1 :b 2)
      (should= 1 (core/get :a))
      (should= 2 (core/get :b)))

    (it "get with default returns default when key missing"
      (should= :nope (core/get :missing :nope)))

    (it "assoc-in! sets nested values"
      (core/assoc-in! [:user :name] "bob")
      (should= "bob" (core/get-in [:user :name])))

    (it "update! applies function to value"
      (core/assoc! :count 0)
      (core/update! :count inc)
      (should= 1 (core/get :count)))

    (it "update-in! applies function to nested value"
      (core/assoc-in! [:stats :hits] 5)
      (core/update-in! [:stats :hits] + 3)
      (should= 8 (core/get-in [:stats :hits])))

    (it "dissoc! removes a key"
      (core/assoc! :temp true)
      (core/dissoc! :temp)
      (should-be-nil (core/get :temp)))

    (it "swap! applies function to entire state"
      (core/swap! merge {:x 1 :y 2})
      (should= 1 (core/get :x))
      (should= 2 (core/get :y)))

     (it "preserves internal :_gherclj key across user operations"
       (core/reset!)
       (core/assoc! :user-data "hello")
       (should-not-be-nil (core/get-in [:_gherclj]))))

  (context "lifecycle hooks"

    (before
      (core/clear-lifecycle-hooks!))

    (it "runs registered hooks in registration order"
      (let [events (atom [])
            record! #(swap! events conj %)]
        (core/before-feature (fn [] (record! :first)))
        (core/before-feature (fn [] (record! :second)))

        (core/run-before-feature-hooks!)

        (should= [:first :second] @events)))

    (it "does not clear registered lifecycle hooks on state reset"
      (let [events (atom [])]
        (core/after-scenario #(swap! events conj :cleanup))

        (core/reset!)
        (core/run-after-scenario-hooks!)

        (should= [:cleanup] @events)))))

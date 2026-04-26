(ns gherclj.core-spec
  (:require [speclj.core :refer :all]
             [gherclj.core :as core :refer [defgiven defwhen defthen helper!]]
             [gherclj.lifecycle :as lifecycle]))

(helper! gherclj.core-spec)

;; Sample helpers (real defns) and step routing entries

(defn setup-project [_slug _timeout] :setup-result)
(defn documented-setup [] :documented-setup-result)
(defn check-zombies [] :action-result)
(defn poll-docs [] :documented-action-result)
(defn assert-zombie [_session-id _reason] :assert-result)
(defn assert-docs [] :documented-assert-result)
(defn assert-headers [_headers-str] :regex-result)

(defgiven "a project {slug:string} with timeout {timeout:int}" core-spec/setup-project)
(defgiven "a documented project step" core-spec/documented-setup
  "Sets project state in memory.")
(defwhen  "checking for zombies" core-spec/check-zombies)
(defwhen  "waiting for the project docs" core-spec/poll-docs
  "Polls for up to 2s.")
(defthen  "session {session-id:string} should be a zombie with reason {reason:string}" core-spec/assert-zombie)
(defthen  "the project docs should match" core-spec/assert-docs
  "Matches within 2s timeout.")
(defthen  #"^the output should contain headers (.+)$" core-spec/assert-headers)

(describe "Core"

  (context "defgiven"

    (it "registers a :given step keyed by helper-ref"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= 'core-spec/setup-project (:helper-ref %)) steps))]
        (should-not-be-nil step)
        (should= :given (:type step))
        (should= "a project {slug:string} with timeout {timeout:int}" (:template step))
        (should-be-nil (:doc step))))

    (it "stores an optional docstring on the registered step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= 'core-spec/documented-setup (:helper-ref %)) steps))]
        (should-not-be-nil step)
        (should= "Sets project state in memory." (:doc step)))))

  (context "defwhen"

    (it "registers a :when step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= 'core-spec/check-zombies (:helper-ref %)) steps))]
        (should-not-be-nil step)
        (should= :when (:type step))))

    (it "stores an optional docstring for a :when step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= 'core-spec/poll-docs (:helper-ref %)) steps))]
        (should-not-be-nil step)
        (should= "Polls for up to 2s." (:doc step)))))

  (context "defthen"

    (it "registers a :then step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= 'core-spec/assert-zombie (:helper-ref %)) steps))]
        (should-not-be-nil step)
        (should= :then (:type step))))

    (it "stores an optional docstring for a :then step"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= 'core-spec/assert-docs (:helper-ref %)) steps))]
        (should-not-be-nil step)
        (should= "Matches within 2s timeout." (:doc step)))))

  (context "raw regex escape hatch"

    (it "accepts a regex pattern instead of a template string"
      (let [steps (core/steps-in-ns 'gherclj.core-spec)
            step (first (filter #(= 'core-spec/assert-headers (:helper-ref %)) steps))]
        (should-not-be-nil step)
        (should-be-nil (:template step))
        (should-not-be-nil (:regex step)))))

  (context "step renderer"

    (it "produces a form invoking the helper-ref with matched args"
      (let [steps (core/collect-steps ['gherclj.core-spec])
            classified (core/classify-step steps "a project \"alpha\" with timeout 300")
            renderer (:renderer classified)]
        (should= '(core-spec/setup-project "alpha" 300)
                 (apply renderer (:args classified))))))

  (context "helper imports"

    (it "records the namespace declared via helper!"
      (should (some #{'gherclj.core-spec} (core/helper-imports-in-ns 'gherclj.core-spec)))))

  (context "classify-step"

    (it "returns all matches for an ambiguous phrase without throwing"
      (let [steps [{:name "greet-any" :regex #"^hello (\S+)$"
                    :bindings [{:name "name" :type "word" :coerce identity}]
                    :ns 'test :type :given}
                   {:name "greet-world" :regex #"^hello world$"
                    :ns 'test :type :given}]
            matches (core/classify-all steps "hello world")]
        (should= ["greet-any" "greet-world"] (mapv :name matches))))

    (it "matches step text to a registered step and extracts args"
      (let [steps (core/collect-steps ['gherclj.core-spec])
            result (core/classify-step steps "a project \"alpha\" with timeout 300")]
        (should-not-be-nil result)
        (should= 'gherclj.core-spec (:ns result))
        (should= 'core-spec/setup-project (:helper-ref result))
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
      (lifecycle/clear!))

    (it "runs registered hooks in registration order"
      (let [events (atom [])
            record! #(swap! events conj %)]
        (core/before-feature (fn [] (record! :first)))
        (core/before-feature (fn [] (record! :second)))

        (lifecycle/run-before-feature-hooks!)

        (should= [:first :second] @events)))

    (it "does not clear registered lifecycle hooks on state reset"
      (let [events (atom [])]
        (core/after-scenario #(swap! events conj :cleanup))

        (core/reset!)
        (lifecycle/run-after-scenario-hooks!)

        (should= [:cleanup] @events)))))
